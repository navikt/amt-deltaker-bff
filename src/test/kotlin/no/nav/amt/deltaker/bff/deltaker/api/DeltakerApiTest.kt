package no.nav.amt.deltaker.bff.deltaker.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
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
import no.nav.amt.deltaker.bff.deltaker.api.model.Begrunnelse
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerlisteDTO
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForslagRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.generateJWT
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
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
        client.post("/deltaker") { postRequest(pameldingRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}") { postRequest(forslagRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.delete("/pamelding/${UUID.randomUUID()}") { deleteRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste autorisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.post("/deltaker") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.delete("/pamelding/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `post deltaker - har tilgang - returnerer deltaker`() = testApplication {
        val deltakerResponse = getDeltakerResponse()
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.opprettDeltaker(any(), any(), any()) } returns deltakerResponse
        setUpTestApplication()
        client.post("/deltaker") { postRequest(pameldingRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(deltakerResponse), bodyAsText())
        }
    }

    @Test
    fun `post deltaker - deltakerliste finnes ikke - reurnerer 404`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every {
            deltakerService.opprettDeltaker(
                any(),
                any(),
                any(),
            )
        } throws NoSuchElementException("Fant ikke deltakerliste")
        setUpTestApplication()
        client.post("/deltaker") { postRequest(pameldingRequest) }.apply {
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

    @Test
    fun `pamelding deltakerId uten godkjenning - har tilgang - oppretter ferdig godkjent deltaker og returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker()
        every { deltakerService.get(deltaker.id) } returns deltaker
        every { deltakerService.meldPaUtenGodkjenning(deltaker, any(), any()) } returns Unit

        setUpTestApplication()
        client.post("/pamelding/${deltaker.id}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `pamelding deltakerId uten godkjenning - deltaker finnes ikke - returnerer 404`() = testApplication {
        every { deltakerService.get(any()) } throws NoSuchElementException()

        setUpTestApplication()
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `slett utkast - har tilgang, deltaker er UTKAST - sletter deltaker og returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST))
        every { deltakerService.get(deltaker.id) } returns deltaker
        every { deltakerService.slettUtkast(deltaker.id) } returns Unit

        setUpTestApplication()
        client.delete("/pamelding/${deltaker.id}") { deleteRequest() }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `slett utkast - deltaker har ikke status UTKAST - returnerer 400`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER))
        every { deltakerService.get(deltaker.id) } returns deltaker

        setUpTestApplication()
        client.delete("/pamelding/${deltaker.id}") { deleteRequest() }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker()
        every { deltakerService.get(deltaker.id) } returns deltaker
        val oppdatertDeltakerResponse = getDeltakerResponse(deltakerId = deltaker.id, statustype = DeltakerStatus.Type.VENTER_PA_OPPSTART, bakgrunnsinformasjon = bakgrunnsinformasjonRequest.bakgrunnsinformasjon)
        every { deltakerService.oppdaterDeltaker(deltaker, DeltakerEndringType.BAKGRUNNSINFORMASJON, any(), any()) } returns oppdatertDeltakerResponse

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(oppdatertDeltakerResponse), bodyAsText())
        }
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - deltaker har sluttet - returnerer bad request`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET), sluttdato = LocalDate.now().minusMonths(1))
        every { deltakerService.get(deltaker.id) } returns deltaker
        every { deltakerService.oppdaterDeltaker(deltaker, DeltakerEndringType.BAKGRUNNSINFORMASJON, any(), any()) } throws IllegalArgumentException("Deltaker har sluttet")

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
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

    private fun HttpRequestBuilder.deleteRequest() {
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
    }

    private fun getDeltakerResponse(
        deltakerId: UUID = UUID.randomUUID(),
        statustype: DeltakerStatus.Type = DeltakerStatus.Type.UTKAST,
        startdato: LocalDate? = null,
        sluttdato: LocalDate? = null,
        dagerPerUke: Float? = null,
        deltakelsesprosent: Float? = null,
        bakgrunnsinformasjon: String? = null,
        mal: List<Mal> = emptyList(),
    ): DeltakerResponse =
        DeltakerResponse(
            deltakerId = deltakerId,
            deltakerliste = DeltakerlisteDTO(
                deltakerlisteId = UUID.randomUUID(),
                deltakerlisteNavn = "Gjennomføring 1",
                tiltakstype = Tiltak.Type.GRUFAGYRKE,
                arrangorNavn = "Arrangør AS",
                oppstartstype = Deltakerliste.Oppstartstype.FELLES,
            ),
            status = TestData.lagDeltakerStatus(type = statustype),
            startdato = startdato,
            sluttdato = sluttdato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = deltakelsesprosent,
            bakgrunnsinformasjon = bakgrunnsinformasjon,
            mal = mal,
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
    private val pameldingUtenGodkjenningRequest = PameldingUtenGodkjenningRequest(
        emptyList(),
        "Bakgrunnen for...",
        null,
        null,
        Begrunnelse("TELEFONKONTAKT", null),
    )
    private val bakgrunnsinformasjonRequest = EndreBakgrunnsinformasjonRequest("Oppdatert bakgrunnsinformasjon")
}
