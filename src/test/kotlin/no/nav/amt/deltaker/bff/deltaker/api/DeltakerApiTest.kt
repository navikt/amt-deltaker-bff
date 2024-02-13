package no.nav.amt.deltaker.bff.deltaker.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import no.nav.amt.deltaker.bff.deltaker.DeltakerHistorikkService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.api.utils.postRequest
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Innhold
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
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
    private val pameldingService = mockk<PameldingService>()
    private val deltakerHistorikkService = mockk<DeltakerHistorikkService>()
    private val navAnsattService = mockk<NavAnsattService>()
    private val navEnhetService = mockk<NavEnhetService>()

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
        coEvery { deltakerService.get(any()) } returns Result.success(TestData.lagDeltaker())

        setUpTestApplication()
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/innhold") { postRequest(innholdRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/startdato") { postRequest(startdatoRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/ikke-aktuell") { postRequest(ikkeAktuellRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/forleng") { postRequest(forlengDeltakelseRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.get("/deltaker/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.get("/deltaker/${UUID.randomUUID()}/historikk") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste autorisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/innhold") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/deltakelsesmengde") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/startdato") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/ikke-aktuell") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/forleng") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.get("/deltaker/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.get("/deltaker/${UUID.randomUUID()}/historikk").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            bakgrunnsinformasjon = bakgrunnsinformasjonRequest.bakgrunnsinformasjon,
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltaker

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
            .apply {
                TestCase.assertEquals(HttpStatusCode.OK, status)
                TestCase.assertEquals(
                    objectMapper.writeValueAsString(oppdatertDeltaker.toDeltakerResponse()),
                    bodyAsText(),
                )
            }
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - deltaker har sluttet - returnerer bad request`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = LocalDate.now().minusMonths(1),
        )
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                any(),
                any(),
                any(),
            )
        } throws IllegalArgumentException("Deltaker har sluttet")

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
            .apply {
                status shouldBe HttpStatusCode.BadRequest
            }
    }

    @Test
    fun `oppdater innhold - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltakerResponse = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            innhold = innholdRequest.innhold,
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.INNHOLD,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltakerResponse

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/innhold") { postRequest(innholdRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(
                objectMapper.writeValueAsString(oppdatertDeltakerResponse.toDeltakerResponse()),
                bodyAsText(),
            )
        }
    }

    @Test
    fun `oppdater deltakelsesmengde - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltakerResponse = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            dagerPerUke = deltakelsesmengdeRequest.dagerPerUke?.toFloat(),
            deltakelsesprosent = deltakelsesmengdeRequest.deltakelsesprosent?.toFloat(),
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.DELTAKELSESMENGDE,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltakerResponse

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(
                objectMapper.writeValueAsString(oppdatertDeltakerResponse.toDeltakerResponse()),
                bodyAsText(),
            )
        }
    }

    @Test
    fun `oppdater startdato - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = startdatoRequest.startdato,
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.STARTDATO,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltaker

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/startdato") { postRequest(startdatoRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(oppdatertDeltaker.toDeltakerResponse()), bodyAsText())
        }
    }

    @Test
    fun `ikke aktuell - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.IKKE_AKTUELL,
                ikkeAktuellRequest.aarsak.toDeltakerStatusAarsak(),
            ),
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.IKKE_AKTUELL,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltaker

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/ikke-aktuell") { postRequest(ikkeAktuellRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(oppdatertDeltaker.toDeltakerResponse())
        }
    }

    @Test
    fun `getDeltaker - har tilgang, deltaker finnes - returnerer deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.navIdent }
        val navEnhet = TestData.lagNavEnhet(enhetsnummer = deltaker.vedtaksinformasjon!!.sistEndretAvEnhet!!)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        every { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        every { navEnhetService.hentEnhet(navEnhet.enhetsnummer) } returns navEnhet

        setUpTestApplication()
        client.get("/deltaker/${deltaker.id}") { noBodyRequest() }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(
                objectMapper.writeValueAsString(deltaker.toDeltakerResponse(ansatte, navEnhet)),
                bodyAsText(),
            )
        }
    }

    @Test
    fun `getDeltakerHistorikk - har tilgang, deltaker finnes - returnerer historikk`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker()
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val vedtak = DeltakerHistorikk.Vedtak(TestData.lagVedtak())
        val endring = DeltakerHistorikk.Endring(TestData.lagDeltakerEndring())
        val historikk = listOf(vedtak, endring)

        val ansatte = TestData.lagNavAnsatteForHistorikk(historikk).associateBy { it.navIdent }

        every { navAnsattService.hentAnsatteForHistorikk(historikk) } returns ansatte

        every { deltakerHistorikkService.getForDeltaker(deltaker.id) } returns historikk

        setUpTestApplication()
        client.get("/deltaker/${deltaker.id}/historikk") { noBodyRequest() }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(historikk.toResponse(ansatte))
        }
    }

    @Test
    fun `forleng - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusDays(3),
        )
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato,
        )
        coEvery {
            deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.FORLENGELSE,
                any(),
                any(),
                any(),
            )
        } returns oppdatertDeltaker

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(objectMapper.writeValueAsString(oppdatertDeltaker.toDeltakerResponse()), bodyAsText())
        }
    }

    @Test
    fun `forleng - har tilgang, ny dato tidligere enn forrige dato - feiler`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(sluttdato = forlengDeltakelseRequest.sluttdato.plusDays(5))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `forleng - har tilgang, har sluttet for mer enn to måneder siden - feiler`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusMonths(3),
        )
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        setUpTestApplication()
        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    private fun HttpRequestBuilder.noBodyRequest() {
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

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(
                tilgangskontrollService,
                deltakerService,
                pameldingService,
                deltakerHistorikkService,
                navAnsattService,
                navEnhetService,
            )
        }
    }

    private val bakgrunnsinformasjonRequest = EndreBakgrunnsinformasjonRequest("Oppdatert bakgrunnsinformasjon")
    private val innholdRequest = EndreInnholdRequest(listOf(Innhold("visningstekst", "type", true, null)))
    private val deltakelsesmengdeRequest = EndreDeltakelsesmengdeRequest(deltakelsesprosent = 50, dagerPerUke = 3)
    private val startdatoRequest = EndreStartdatoRequest(LocalDate.now().plusWeeks(1))
    private val ikkeAktuellRequest = IkkeAktuellRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB))
    private val forlengDeltakelseRequest = ForlengDeltakelseRequest(LocalDate.now().plusWeeks(3))
}
