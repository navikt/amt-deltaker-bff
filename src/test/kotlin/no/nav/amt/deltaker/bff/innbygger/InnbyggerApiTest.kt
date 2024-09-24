package no.nav.amt.deltaker.bff.innbygger

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
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
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.innbygger.model.toInnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.tokenXToken
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class InnbyggerApiTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()
    private val tilgangskontrollService = TilgangskontrollService(poaoTilgangCachedClient)
    private val deltakerService = mockk<DeltakerService>(relaxUnitFun = true)
    private val pameldingService = mockk<PameldingService>()
    private val navAnsattService = mockk<NavAnsattService>()
    private val navEnhetService = mockk<NavEnhetService>()
    private val forslagService = mockk<ForslagService>()
    private val innbyggerService = mockk<InnbyggerService>()
    private val amtDistribusjonClient = mockk<AmtDistribusjonClient>()

    @Before
    fun setup() {
        configureEnvForAuthentication()
    }

    @Test
    fun `skal teste tilgangskontroll - har ikke tilgang - returnerer 403`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(
            null,
            Decision.Deny("Ikke tilgang", ""),
        )
        coEvery { deltakerService.get(any()) } returns Result.success(TestData.lagDeltaker())

        setUpTestApplication()
        client.get("/innbygger/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.post("/innbygger/${UUID.randomUUID()}/godkjenn-utkast") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.get("/innbygger/${UUID.randomUUID()}/historikk") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste tilgangskontroll - mangler token - returnerer 401`() = testApplication {
        coEvery { deltakerService.get(any()) } returns Result.success(TestData.lagDeltaker())

        setUpTestApplication()
        client.get("/innbygger/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.post("/innbygger/${UUID.randomUUID()}/godkjenn-utkast").status shouldBe HttpStatusCode.Unauthorized
        client.get("/innbygger/${UUID.randomUUID()}/historikk").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `get id - innbygger har tilgang - returnerer 200 og deltaker`() {
        val deltaker = TestData.lagDeltaker()
        val forslag = TestData.lagForslag(deltakerId = deltaker.id)

        mockTestApi(deltaker, forslag = listOf(forslag)) { client, ansatte, enhet ->
            val res = client.get("/innbygger/${deltaker.id}") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.OK
            res.bodyAsText() shouldBe objectMapper.writeValueAsString(
                deltaker.toInnbyggerDeltakerResponse(
                    ansatte,
                    enhet,
                    listOf(forslag),
                ),
            )
        }
    }

    @Test
    fun `get id - deltaker finnes ikke - returnerer 404`() {
        coEvery { deltakerService.get(any()) } returns Result.failure(NoSuchElementException())

        testApplication {
            setUpTestApplication()
            val res = client.get("/innbygger/${UUID.randomUUID()}") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `godkjenn-utkast - deltaker har feil status - feiler`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))

        mockTestApi(deltaker) { client, _, _ ->
            val res = client.post("/innbygger/${deltaker.id}/godkjenn-utkast") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `godkjenn-utkast - deltaker finnes ikke - returnerer 404`() {
        coEvery { deltakerService.get(any()) } returns Result.failure(NoSuchElementException())

        testApplication {
            setUpTestApplication()
            val res = client.get("/innbygger/${UUID.randomUUID()}/godkjenn-utkast") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `godkjenn-utkast - deltaker har tilgang - fatter vedtak`() {
        val deltaker = deltakerMedIkkeFattetVedtak()
        val deltakerMedFattetVedak = deltaker.fattVedtak()

        coEvery { innbyggerService.fattVedtak(deltaker) } returns deltakerMedFattetVedak

        mockTestApi(deltaker, deltakerMedFattetVedak) { client, ansatte, enhet ->
            val res = client.post("/innbygger/${deltaker.id}/godkjenn-utkast") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.OK
            res.bodyAsText() shouldBe objectMapper.writeValueAsString(
                deltakerMedFattetVedak.toInnbyggerDeltakerResponse(
                    ansatte,
                    enhet,
                    emptyList(),
                ),
            )
        }
    }

    @Test
    fun `getHistorikk - deltaker finnes, har tilgang - returnerer historikk`() {
        val deltaker = TestData.lagDeltaker().let { TestData.leggTilHistorikk(it, 2, 2, 1) }

        mockTestApi(deltaker, null) { client, _, _ ->
            val historikk = deltaker.getDeltakerHistorikkSortert()
            val ansatte = TestData.lagNavAnsatteForHistorikk(historikk).associateBy { it.id }
            val enheter = TestData.lagNavEnheterForHistorikk(historikk).associateBy { it.id }

            every { navAnsattService.hentAnsatteForHistorikk(historikk) } returns ansatte
            every { navEnhetService.hentEnheterForHistorikk(historikk) } returns enheter
            client.get("/innbygger/${deltaker.id}/historikk") { noBodyRequest() }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writePolymorphicListAsString(
                    historikk.toResponse(ansatte, deltaker.deltakerliste.arrangor.getArrangorNavn(), enheter),
                )
            }
        }
    }

    private fun HttpRequestBuilder.noBodyRequest() {
        header(
            HttpHeaders.Authorization,
            "Bearer ${tokenXToken("personident", Environment())}",
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
                navAnsattService,
                navEnhetService,
                innbyggerService,
                forslagService,
                amtDistribusjonClient,
                mockk(),
            )
        }
    }

    private fun mockTestApi(
        deltaker: Deltaker,
        oppdatertDeltaker: Deltaker? = null,
        forslag: List<Forslag> = emptyList(),
        block: suspend (client: HttpClient, ansatte: Map<UUID, NavAnsatt>, enhet: NavEnhet?) -> Unit,
    ) = testApplication {
        setUpTestApplication()

        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        every { forslagService.getForDeltaker(deltaker.id) } returns forslag

        val (ansatte, enhet) = if (oppdatertDeltaker == null) {
            mockAnsatteOgEnhetForDeltaker(deltaker)
        } else {
            mockAnsatteOgEnhetForDeltaker(oppdatertDeltaker)
        }

        block(client, ansatte, enhet)
    }

    private fun mockAnsatteOgEnhetForDeltaker(deltaker: Deltaker): Pair<Map<UUID, NavAnsatt>, NavEnhet?> {
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.id }
        val enhet = deltaker.vedtaksinformasjon?.let { TestData.lagNavEnhet(id = it.sistEndretAvEnhet) }
        val enheter = TestData.lagNavEnheterForHistorikk(deltaker.historikk).associateBy { it.id }

        every { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        enhet?.let { every { navEnhetService.hentEnhet(it.id) } returns it }
        every { navEnhetService.hentEnheterForHistorikk(any()) } returns enheter

        return Pair(ansatte, enhet)
    }
}

fun deltakerMedIkkeFattetVedtak(): Deltaker {
    val deltaker = TestData.lagDeltaker(
        status = TestData.lagDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        historikk = false,
    )
    val vedtak = TestData.lagVedtak(deltakerVedVedtak = deltaker, fattet = null)

    return deltaker.copy(historikk = listOf(DeltakerHistorikk.Vedtak(vedtak)))
}

fun Deltaker.fattVedtak(): Deltaker {
    val vedtak = this.ikkeFattetVedtak!!

    return this.copy(
        historikk = this.historikk
            .filter { it.id != vedtak.id }
            .plus(
                DeltakerHistorikk.Vedtak(
                    vedtak.copy(
                        fattet = LocalDateTime.now(),
                        sistEndret = LocalDateTime.now(),
                    ),
                ),
            ),
    )
}

private val DeltakerHistorikk.id
    get() = when (this) {
        is DeltakerHistorikk.Endring -> endring.id
        is DeltakerHistorikk.Vedtak -> vedtak.id
        is DeltakerHistorikk.Forslag -> forslag.id
        is DeltakerHistorikk.EndringFraArrangor -> endringFraArrangor.id
        is DeltakerHistorikk.ImportertFraArena -> importertFraArena.deltakerId
    }
