package no.nav.amt.deltaker.bff.deltaker.api

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
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
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.api.model.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.api.utils.postRequest
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
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
    private val navAnsattService = mockk<NavAnsattService>()
    private val navEnhetService = mockk<NavEnhetService>()

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
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/innhold") { postRequest(innholdRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/startdato") { postRequest(startdatoRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/sluttdato") { postRequest(sluttdatoRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/ikke-aktuell") { postRequest(ikkeAktuellRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/forleng") { postRequest(forlengDeltakelseRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/avslutt") { postRequest(avsluttDeltakelseRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.get("/deltaker/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.get("/deltaker/${UUID.randomUUID()}/historikk") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste autentisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/innhold") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/deltakelsesmengde") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/startdato") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/sluttdato") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/ikke-aktuell") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/forleng") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/avslutt") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.get("/deltaker/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.get("/deltaker/${UUID.randomUUID()}/historikk").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            bakgrunnsinformasjon = bakgrunnsinformasjonRequest.bakgrunnsinformasjon,
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
                .apply {
                    status shouldBe HttpStatusCode.OK
                    bodyAsText() shouldBe objectMapper.writeValueAsString(
                        oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                    )
                }
        }
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - deltaker har sluttet - returnerer bad request`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = LocalDate.now().minusMonths(1),
        )

        mockTestApi(deltaker, null) { client, _, _ ->
            client.post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
                .apply {
                    status shouldBe HttpStatusCode.BadRequest
                }
        }
    }

    @Test
    fun `oppdater innhold - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            innhold = finnValgtInnhold(innholdRequest.innhold, deltaker),
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/innhold") {
                postRequest(EndreInnholdRequest(listOf(InnholdDto(deltaker.innhold[0].innholdskode, null))))
            }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `oppdater deltakelsesmengde - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            dagerPerUke = deltakelsesmengdeRequest.dagerPerUke?.toFloat(),
            deltakelsesprosent = deltakelsesmengdeRequest.deltakelsesprosent?.toFloat(),
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `oppdater startdato - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = startdatoRequest.startdato,
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/startdato") { postRequest(startdatoRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `endre sluttdato - har tilgang, deltaker har status HAR SLUTTET - returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = LocalDate.now().minusDays(3),
        )
        val oppdatertDeltaker = deltaker.copy(
            sluttdato = sluttdatoRequest.sluttdato,
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/sluttdato") { postRequest(sluttdatoRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `endre sluttdato - har tilgang, deltaker har status IKKE AKTUELL - feiler`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            sluttdato = LocalDate.now().minusDays(3),
        )

        mockTestApi(deltaker, null) { client, _, _ ->
            client.post("/deltaker/${deltaker.id}/sluttdato") { postRequest(sluttdatoRequest) }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `ikke aktuell - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.IKKE_AKTUELL,
                ikkeAktuellRequest.aarsak.toDeltakerStatusAarsak(),
            ),
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/ikke-aktuell") { postRequest(ikkeAktuellRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `endre sluttarsak - har tilgang, deltaker har status HAR SLUTTET - returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, aarsak = null),
        )

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = sluttarsakRequest.aarsak.toDeltakerStatusAarsak(),
            ),
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/sluttarsak") { postRequest(sluttarsakRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(
                        ansatte,
                        enhet,
                    ),
                )
            }
        }
    }

    @Test
    fun `getDeltaker - har tilgang, deltaker finnes - returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))

        mockTestApi(deltaker, null) { client, ansatte, enhet ->
            client.get("/deltaker/${deltaker.id}") { noBodyRequest() }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltaker.toDeltakerResponse(ansatte, enhet))
            }
        }
    }

    @Test
    fun `getDeltakerHistorikk - har tilgang, deltaker finnes - returnerer historikk`() {
        val deltaker = TestData.lagDeltaker().let { TestData.leggTilHistorikk(it, 2, 2) }

        mockTestApi(deltaker, null) { client, _, enhet ->
            val historikk = deltaker.getDeltakerHistorikSortert()
            val ansatte = TestData.lagNavAnsatteForHistorikk(historikk).associateBy { it.id }

            every { navAnsattService.hentAnsatteForHistorikk(historikk) } returns ansatte
            client.get("/deltaker/${deltaker.id}/historikk") { noBodyRequest() }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(historikk.toResponse(ansatte))
            }
        }
    }

    @Test
    fun `forleng - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusDays(3),
        )
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato,
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `forleng - har tilgang, ny dato tidligere enn forrige dato - feiler`() {
        val deltaker = TestData.lagDeltaker(sluttdato = forlengDeltakelseRequest.sluttdato.plusDays(5))

        mockTestApi(deltaker, null) { client, _, _ ->
            client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `forleng - har tilgang, har sluttet for mer enn to måneder siden - feiler`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusMonths(3),
        )

        mockTestApi(deltaker, null) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `avslutt - har tilgang - returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.HAR_SLUTTET,
                avsluttDeltakelseRequest.aarsak.toDeltakerStatusAarsak(),
            ),
            sluttdato = avsluttDeltakelseRequest.sluttdato,
        )

        mockTestApi(deltaker, oppdatertDeltaker) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequest) }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet),
                )
            }
        }
    }

    @Test
    fun `avslutt - har tilgang, status VENTER PA OPPSTART - feiler`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))

        mockTestApi(deltaker, null) { client, ansatte, enhet ->
            client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequest) }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
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
                navAnsattService,
                navEnhetService,
            )
        }
    }

    private val bakgrunnsinformasjonRequest = EndreBakgrunnsinformasjonRequest("Oppdatert bakgrunnsinformasjon")
    private val innholdRequest = EndreInnholdRequest(listOf(InnholdDto("annet", "beskrivelse")))
    private val deltakelsesmengdeRequest = EndreDeltakelsesmengdeRequest(deltakelsesprosent = 50, dagerPerUke = 3)
    private val startdatoRequest = EndreStartdatoRequest(LocalDate.now().plusWeeks(1))
    private val ikkeAktuellRequest = IkkeAktuellRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB))
    private val forlengDeltakelseRequest = ForlengDeltakelseRequest(LocalDate.now().plusWeeks(3))
    private val avsluttDeltakelseRequest =
        AvsluttDeltakelseRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB), LocalDate.now())
    private val sluttdatoRequest = EndreSluttdatoRequest(LocalDate.now().minusDays(1))
    private val sluttarsakRequest =
        EndreSluttarsakRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT))

    private fun mockTestApi(
        deltaker: Deltaker,
        oppdatertDeltaker: Deltaker?,
        block: suspend (client: HttpClient, ansatte: Map<UUID, NavAnsatt>, enhet: NavEnhet?) -> Unit,
    ) = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        val (ansatte, enhet) = if (oppdatertDeltaker != null) {
            coEvery {
                deltakerService.oppdaterDeltaker(deltaker, any(), any(), any())
            } returns oppdatertDeltaker

            mockAnsatteOgEnhetForDeltaker(oppdatertDeltaker)
        } else {
            mockAnsatteOgEnhetForDeltaker(deltaker)
        }
        setUpTestApplication()
        block(client, ansatte, enhet)
    }

    private fun mockAnsatteOgEnhetForDeltaker(
        deltaker: Deltaker,
    ): Pair<Map<UUID, NavAnsatt>, NavEnhet?> {
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.id }
        val enhet = deltaker.vedtaksinformasjon?.let { TestData.lagNavEnhet(id = it.sistEndretAvEnhet) }

        every { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        enhet?.let { every { navEnhetService.hentEnhet(it.id) } returns it }

        return Pair(ansatte, enhet)
    }
}
