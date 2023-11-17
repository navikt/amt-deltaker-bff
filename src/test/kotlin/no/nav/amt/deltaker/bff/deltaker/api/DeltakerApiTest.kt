package no.nav.amt.deltaker.bff.deltaker.api

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
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
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
    fun `pamelding - har tilgang - returnerer PameldingResponse`() = testApplication {
        val pameldingResponse = getPameldingResponse()
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.opprettDeltaker(any(), any(), any()) } returns pameldingResponse
        setUpTestApplication()
        client.post("/pamelding") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff")}")
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(PameldingRequest(UUID.randomUUID(), "1234")))
        }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(pameldingResponse), bodyAsText())
        }
    }

    @Test
    fun `pamelding - har ikke tilgang - returnerer 403`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))
        setUpTestApplication()
        client.post("/pamelding") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff")}")
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(PameldingRequest(UUID.randomUUID(), "1234")))
        }.apply {
            TestCase.assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `pamelding - deltakerliste finnes ikke - reurnerer 404`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.opprettDeltaker(any(), any(), any()) } throws NoSuchElementException("Fant ikke deltakerliste")
        setUpTestApplication()
        client.post("/pamelding") {
            header(HttpHeaders.Authorization, "Bearer ${generateJWT("frontend-clientid", UUID.randomUUID().toString(), "deltaker-bff")}")
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(PameldingRequest(UUID.randomUUID(), "1234")))
        }.apply {
            TestCase.assertEquals(HttpStatusCode.NotFound, status)
        }
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
            configureAuthentication(Environment())
            configureRouting(tilgangskontrollService, deltakerService)
        }
    }
}
