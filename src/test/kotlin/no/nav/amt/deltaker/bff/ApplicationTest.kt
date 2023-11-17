package no.nav.amt.deltaker.bff

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import org.junit.Test

class ApplicationTest {
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val deltakerService = mockk<DeltakerService>()

    @Test
    fun testRoot() = testApplication {
        configureEnvForAuthentication()
        application {
            configureAuthentication(Environment())
            configureRouting(tilgangskontrollService, deltakerService)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }
}
