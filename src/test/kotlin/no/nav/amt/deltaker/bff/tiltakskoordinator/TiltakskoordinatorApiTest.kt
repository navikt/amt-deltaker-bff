package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyRequest
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TiltakskoordinatorApiTest {
    private val deltakerService = mockk<DeltakerService>()
    private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()

    @Before
    fun setup() {
        configureEnvForAuthentication()
    }

    @Test
    fun `skal teste autentisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `get deltakerliste - liste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        every { deltakerlisteRepository.get(any()) } returns Result.failure(NoSuchElementException())
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `get deltakerliste - liste finnes - returnerer 200 og liste`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteRepository.get(deltakerliste.id) } returns Result.success(deltakerliste)
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltakerliste.toResponse())
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes ikke - returnerer tom liste`() = testApplication {
        setUpTestApplication()
        every { deltakerService.getForDeltakerliste(any()) } returns emptyList()
        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(listOf<DeltakerResponse>())
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes - returnerer liste med deltakere`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        val deltakere = (0..5).map { TestData.lagDeltaker(deltakerliste = deltakerliste) }
        every { deltakerService.getForDeltakerliste(deltakerliste.id) } returns deltakere
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltakere.map { it.toDeltakerResponse() })
            }
    }

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(
                mockk(),
                deltakerService,
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                deltakerlisteRepository,
                mockk(),
            )
        }
    }
}
