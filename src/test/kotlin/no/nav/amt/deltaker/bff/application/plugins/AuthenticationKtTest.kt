package no.nav.amt.deltaker.bff.application.plugins

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.utils.generateJWT
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths
import java.util.UUID

class AuthenticationKtTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()
    private val tilgangskontrollService = TilgangskontrollService(poaoTilgangCachedClient)

    @Before
    fun setup() {
        configureEnv()
    }

    @Test
    fun `testAuthentication - gyldig token, ansatt har tilgang - returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        application {
            configureAuthentication(Environment())
            configureRouting()
            setUpTestRoute()
        }
        client.get("/fnr/12345678910") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff")}")
        }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals("Veileder har tilgang!", bodyAsText())
        }
    }

    @Test
    fun `testAuthentication - gyldig token, ansatt har ikke tilgang - returnerer 403`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))
        application {
            configureAuthentication(Environment())
            configureRouting()
            setUpTestRoute()
        }
        client.get("/fnr/12345678910") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff")}")
        }.apply {
            TestCase.assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `testAuthentication - ugyldig tokenissuer - returnerer 401`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        application {
            configureAuthentication(Environment())
            configureRouting()
            setUpTestRoute()
        }
        client.get("/fnr/12345678910") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff", issuer = "annenIssuer")}")
        }.apply {
            TestCase.assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    private fun configureEnv() {
        val path = "src/test/resources/jwkset.json"
        val uri = Paths.get(path).toUri().toURL().toString()
        System.setProperty(Environment.AZURE_OPENID_CONFIG_JWKS_URI_KEY, uri)
        System.setProperty(Environment.AZURE_OPENID_CONFIG_ISSUER_KEY, "issuer")
        System.setProperty(Environment.AZURE_APP_CLIENT_ID_KEY, "deltaker-bff")
    }

    private fun Application.setUpTestRoute() {
        routing {
            authenticate("VEILEDER") {
                get("/fnr/{fnr}") {
                    val norskIdent = call.parameters["fnr"]!!
                    tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), norskIdent)

                    call.respondText("Veileder har tilgang!")
                }
            }
        }
    }
}
