package no.nav.amt.deltaker.bff.deltaker.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.generateJWT
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeltakerApiTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()
    private val tilgangskontrollService = TilgangskontrollService(poaoTilgangCachedClient)
    private val deltakerService = mockk<DeltakerService>()

    @Before
    fun setup() {
        configureEnvForAuthentication()
    }

    @Test
    fun `skal teste autentisering - har ikke tilgang - returnerer 403`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(
            null,
            Decision.Deny("Ikke tilgang", ""),
        )
        coEvery { deltakerService.get(any()) } returns TestData.lagDeltaker()

        setUpTestApplication()
        client.post("/pamelding") { postRequest(pameldingRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}") { postRequest(forslagRequest) }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste autorisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.post("/pamelding") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `pamelding - har tilgang - returnerer PameldingResponse`() = testApplication {
        val pameldingResponse = getPameldingResponse()
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.opprettDeltaker(any(), any(), any()) } returns pameldingResponse
        setUpTestApplication()
        client.post("/pamelding") { postRequest(pameldingRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(pameldingResponse), bodyAsText())
        }
    }

    @Test
    fun `pamelding - deltakerliste finnes ikke - reurnerer 404`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every {
            deltakerService.opprettDeltaker(
                any(),
                any(),
                any(),
            )
        } throws NoSuchElementException("Fant ikke deltakerliste")
        setUpTestApplication()
        client.post("/pamelding") { postRequest(pameldingRequest) }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `pamelding deltakerId - har tilgang - oppretter forslag og returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker()
        every { deltakerService.get(deltaker.id) } returns deltaker
        every { deltakerService.opprettForslag(deltaker, any(), any()) } returns Unit

        setUpTestApplication()
        client.post("/pamelding/${deltaker.id}") { postRequest(forslagRequest) }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `pamelding deltakerId - deltaker finnes ikke - returnerer 404`() = testApplication {
        every { deltakerService.get(any()) } throws NoSuchElementException()

        setUpTestApplication()
        client.post("/pamelding/${UUID.randomUUID()}") { postRequest(forslagRequest) }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    private fun HttpRequestBuilder.postRequest(body: Any) {
        header(
            HttpHeaders.Authorization,
            "Bearer ${
                generateJWT(
                    consumerClientId = "frontend-clientid",
                    navAnsattAzureId = UUID.randomUUID().toString(),
                    audience = "deltaker-bff",
                )
            }",
        )
        contentType(ContentType.Application.Json)
        setBody(objectMapper.writeValueAsString(body))
    }

    private fun getPameldingResponse(): PameldingResponse =
        PameldingResponse(
            deltakerId = UUID.randomUUID(),
            deltakerliste = DeltakerlisteDTO(
                deltakerlisteId = UUID.randomUUID(),
                deltakerlisteNavn = "Gjennomføring 1",
                tiltakstype = Tiltak.Type.GRUFAGYRKE,
                arrangorNavn = "Arrangør AS",
                oppstartstype = Deltakerliste.Oppstartstype.FELLES,
                mal = emptyList(),
            ),
        )

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(tilgangskontrollService, deltakerService)
        }
    }

    private val forslagRequest = ForslagRequest(emptyList(), "Bakgrunnen for...", null, null)
    private val pameldingRequest = PameldingRequest(UUID.randomUUID(), "1234")
}
