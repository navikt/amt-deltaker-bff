package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyTiltakskoordinatorRequest
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.UlestHendelseService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UlestHendelseApiTest {
    private val mockUlestHendelseService = mockk<UlestHendelseService>()

    @BeforeEach
    fun setup() {
        clearAllMocks()
        configureEnvForAuthentication()
    }

    @Test
    fun `skal returnere Unauthorized nar tilgang mangler`() = testApplication {
        setUpTestApplication()

        val response = client.delete("/tiltakskoordinator/ulest-hendelse/${UUID.randomUUID()}")

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `skal returnere NoContent nar hendelse er slettet`() = testApplication {
        setUpTestApplication()
        coEvery { mockUlestHendelseService.delete(any()) } just runs

        val response = client.delete("/tiltakskoordinator/ulest-hendelse/${UUID.randomUUID()}") {
            noBodyTiltakskoordinatorRequest()
        }

        response.status shouldBe HttpStatusCode.NoContent

        coVerify { mockUlestHendelseService.delete(any()) }
    }

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockUlestHendelseService,
                mockk(),
            )
        }
    }
}
