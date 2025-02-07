package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerTilgang
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyRequest
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyTiltakskoordinatorRequest
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TiltakskoordinatorApiTest {
    private val deltakerService = mockk<DeltakerService>()
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val deltakerlisteService = mockk<DeltakerlisteService>()

    @Before
    fun setup() {
        configureEnvForAuthentication()
    }

    @Test
    fun `skal teste autentisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere").status shouldBe HttpStatusCode.Unauthorized
        client.post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/tilgang/legg-til").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `skal teste autentisering - mangler AD rolle - returnerer 401`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.hentMedFellesOppstart(deltakerliste.id) } returns Result.success(deltakerliste)

        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/tilgang/legg-til") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
    }

    @Test
    fun `get deltakerliste - liste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        every { deltakerlisteService.hentMedFellesOppstart(any()) } returns Result.failure(NoSuchElementException())

        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}") {
                noBodyTiltakskoordinatorRequest()
            }.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `get deltakerliste - liste finnes - returnerer 200 og liste`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.hentMedFellesOppstart(deltakerliste.id) } returns Result.success(deltakerliste)
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltakerliste.toResponse(emptyList()))
            }
    }

    @Test
    fun `get deltakere - mangler tilgang til deltakerliste - returnerer 403`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.verifiserDeltakerlisteHarFellesOppstart(deltakerliste.id) } returns deltakerliste
        coEvery { tilgangskontrollService.verifiserTiltakskoordinatorTilgang(any(), any()) } throws AuthorizationException("")
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.Forbidden
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        every { deltakerlisteService.verifiserDeltakerlisteHarFellesOppstart(any()) } throws NoSuchElementException()
        every { deltakerService.getForDeltakerliste(any()) } returns emptyList()
        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.NotFound
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes - returnerer liste med deltakere`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        val deltakerliste = TestData.lagDeltakerliste()
        val deltakere = (0..5).map { TestData.lagDeltaker(deltakerliste = deltakerliste) }
        every { deltakerlisteService.verifiserDeltakerlisteHarFellesOppstart(deltakerliste.id) } returns deltakerliste
        every { deltakerService.getForDeltakerliste(deltakerliste.id) } returns deltakere
        deltakere.forEach {
            every { tilgangskontrollService.vurderKoordinatorTilgangTilDeltaker(any(), it) } returns it.toDeltakerTilgang()
        }

        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltakere.map { it.toDeltakerTilgang().toDeltakerResponse() })
            }
    }

    @Test
    fun `legg til tilgang - har ikke tilgang fra før - returnerer 200`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        coEvery {
            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(
                any(),
                deltakerliste.id,
            )
        } returns Result.success(TestData.lagTiltakskoordinatorTilgang())

        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/tilgang/legg-til") { noBodyTiltakskoordinatorRequest() }
            .status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `legg til tilgang - har tilgang fra før - returnerer 400`() = testApplication {
        setUpTestApplication()
        coEvery {
            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(
                any(),
                any(),
            )
        } returns Result.failure(IllegalArgumentException())

        client
            .post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/tilgang/legg-til") { noBodyTiltakskoordinatorRequest() }
            .status shouldBe HttpStatusCode.BadRequest
    }

    private fun mockTilgangTilDeltakerliste() {
        coEvery { tilgangskontrollService.verifiserTiltakskoordinatorTilgang(any(), any()) } returns Unit
    }

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(
                tilgangskontrollService,
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
                deltakerlisteService,
                mockk(),
            )
        }
    }

    private fun Deltaker.toDeltakerTilgang(tilgang: Boolean = true) = TiltakskoordinatorDeltakerTilgang(this, tilgang)
}
