package no.nav.amt.deltaker.bff.deltaker.api

import com.fasterxml.jackson.module.kotlin.readValue
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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorsDeltakerlisteProducer
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.model.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.AvvisForslagRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.FjernOppstartsdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltaker.api.model.ReaktiverDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.api.utils.postRequest
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.toDeltakerStatusAarsak
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.generateJWT
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakskoordinatorDeltakerApiTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()
    private val deltakerService = mockk<DeltakerService>()
    private val pameldingService = mockk<PameldingService>()
    private val navAnsattService = mockk<NavAnsattService>()
    private val navEnhetService = mockk<NavEnhetService>()
    private val forslagService = mockk<ForslagService>(relaxed = true)
    private val amtDistribusjonClient = mockk<AmtDistribusjonClient>()
    private val sporbarhetsloggService = mockk<SporbarhetsloggService>(relaxed = true)
    private val unleashToggle = mockk<UnleashToggle>()
    private val tiltakskoordinatorTilgangRepository = mockk<TiltakskoordinatorTilgangRepository>()
    private val tiltakskoordinatorsDeltakerlisteProducer = mockk<TiltakskoordinatorsDeltakerlisteProducer>()
    private val deltakerlisteService = mockk<DeltakerlisteService>()
    private val tiltakskoordinatorService = mockk<TiltakskoordinatorService>()
    private val tilgangskontrollService = TilgangskontrollService(
        poaoTilgangCachedClient,
        navAnsattService,
        tiltakskoordinatorTilgangRepository,
        tiltakskoordinatorsDeltakerlisteProducer,
        tiltakskoordinatorService,
        deltakerlisteService,
    )

    @BeforeEach
    fun setup() {
        configureEnvForAuthentication()
        clearMocks(sporbarhetsloggService)
    }

    @Test
    fun `skal teste tilgangskontroll - har ikke tilgang - returnerer 403`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(
            null,
            Decision.Deny("Ikke tilgang", ""),
        )
        coEvery {
            deltakerService.get(
                any(),
            )
        } returns Result.success(TestData.lagDeltaker(navBruker = TestData.lagNavBruker(personident = "1234")))
        every { forslagService.get(any()) } returns Result.success(TestData.lagForslag())
        every { unleashToggle.erKometMasterForTiltakstype(Tiltakstype.ArenaKode.ARBFORB) } returns true
        setUpTestApplication()
        client.post("/deltaker/${UUID.randomUUID()}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }.status shouldBe
            HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/innhold") { postRequest(innholdRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.status shouldBe
            HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/startdato") { postRequest(startdatoRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/deltaker/${UUID.randomUUID()}/sluttdato") { postRequest(sluttdatoRequest) }.status shouldBe HttpStatusCode.Forbidden
        client
            .post(
                "/deltaker/${UUID.randomUUID()}/ikke-aktuell",
            ) { postRequest(ikkeAktuellRequest) }
            .status shouldBe HttpStatusCode.Forbidden
        client
            .post(
                "/deltaker/${UUID.randomUUID()}/forleng",
            ) { postRequest(forlengDeltakelseRequest) }
            .status shouldBe HttpStatusCode.Forbidden
        client
            .post(
                "/deltaker/${UUID.randomUUID()}/avslutt",
            ) { postRequest(avsluttDeltakelseRequest) }
            .status shouldBe HttpStatusCode.Forbidden
        client
            .post("/deltaker/${UUID.randomUUID()}") {
                postRequest(deltakerRequest)
            }.status shouldBe HttpStatusCode.Forbidden
        client.get("/deltaker/${UUID.randomUUID()}/historikk") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client
            .post(
                "/deltaker/${UUID.randomUUID()}/reaktiver",
            ) { postRequest(reaktiverDeltakelseRequest) }
            .status shouldBe HttpStatusCode.Forbidden
        client.post("/forslag/${UUID.randomUUID()}/avvis") { postRequest(avvisForslagRequest) }.status shouldBe HttpStatusCode.Forbidden
        client
            .post("/deltaker/${UUID.randomUUID()}/fjern-oppstartsdato") {
                postRequest(fjernOppstartsdatoRequest)
            }.status shouldBe HttpStatusCode.Forbidden
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
        client.post("/deltaker/${UUID.randomUUID()}/reaktiver") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}/fjern-oppstartsdato") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/deltaker/${UUID.randomUUID()}") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.get("/deltaker/${UUID.randomUUID()}/historikk").status shouldBe HttpStatusCode.Unauthorized
        client.post("/forslag/${UUID.randomUUID()}/avvis").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            bakgrunnsinformasjon = bakgrunnsinformasjonRequest.bakgrunnsinformasjon,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client
            .post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
                )
            }
    }

    @Test
    fun `oppdater bakgrunnsinformasjon - deltaker har sluttet - returnerer bad request`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, gyldigFra = LocalDateTime.now().minusMonths(3)),
            sluttdato = LocalDate.now().minusMonths(1),
        )
        setupMocks(deltaker, null)

        client
            .post("/deltaker/${deltaker.id}/bakgrunnsinformasjon") { postRequest(bakgrunnsinformasjonRequest) }
            .apply {
                status shouldBe HttpStatusCode.BadRequest
            }
    }

    @Test
    fun `oppdater innhold - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            deltakelsesinnhold = Deltakelsesinnhold("ledetekst", finnValgtInnhold(innholdRequest.innhold, deltaker)),
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client
            .post("/deltaker/${deltaker.id}/innhold") {
                postRequest(EndreInnholdRequest(listOf(InnholdDto(deltaker.deltakelsesinnhold!!.innhold[0].innholdskode, null))))
            }.apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
                )
            }
    }

    @Test
    fun `oppdater deltakelsesmengde - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(
                sluttdato = LocalDate.now().plusMonths(3),
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            )

        val oppdatertDeltaker = deltaker.copy(
            dagerPerUke = deltakelsesmengdeRequest.dagerPerUke?.toFloat(),
            deltakelsesprosent = deltakelsesmengdeRequest.deltakelsesprosent?.toFloat(),
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/deltakelsesmengde") { postRequest(deltakelsesmengdeRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `oppdater deltakelsesmengde - ingen endring - returnerer BadRequest`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(
                sluttdato = LocalDate.now().plusMonths(3),
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            )
        setupMocks(deltaker, null)

        client
            .post("/deltaker/${deltaker.id}/deltakelsesmengde") {
                postRequest(
                    EndreDeltakelsesmengdeRequest(
                        deltakelsesprosent = deltaker.deltakelsesprosent?.toInt(),
                        dagerPerUke = deltaker.dagerPerUke?.toInt(),
                        begrunnelse = "begrunnelse",
                        gyldigFra = LocalDate.now(),
                        forslagId = null,
                    ),
                )
            }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
    }

    @Test
    fun `oppdater startdato - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = startdatoRequest.startdato,
            sluttdato = sluttdatoRequest.sluttdato,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/startdato") { postRequest(startdatoRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `endre sluttdato - har tilgang, deltaker har status HAR SLUTTET - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = LocalDate.now().minusDays(3),
        )
        val oppdatertDeltaker = deltaker.copy(
            sluttdato = sluttdatoRequest.sluttdato,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/sluttdato") { postRequest(sluttdatoRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `endre sluttdato - har tilgang, deltaker har status IKKE AKTUELL - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            sluttdato = LocalDate.now().minusDays(3),
        )
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}/sluttdato") { postRequest(sluttdatoRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `ikke aktuell - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.IKKE_AKTUELL,
                ikkeAktuellRequest.aarsak.toDeltakerStatusAarsak(),
            ),
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/ikke-aktuell") { postRequest(ikkeAktuellRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `endre sluttarsak - har tilgang, deltaker har status HAR SLUTTET - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, aarsak = null),
        )

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = sluttarsakRequest.aarsak.toDeltakerStatusAarsak(),
            ),
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/sluttarsak") { postRequest(sluttarsakRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(
                    ansatte,
                    enhet,
                    true,
                    emptyList(),
                ),
            )
        }
    }

    @Test
    fun `getDeltaker - har tilgang, deltaker finnes - returnerer deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            navBruker = TestData.lagNavBruker(personident = "1234"),
        )
        val (ansatte, enhet) = setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}") { postRequest(deltakerRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(deltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()))
        }
        coVerify(exactly = 1) { sporbarhetsloggService.sendAuditLog(any(), any()) }
    }

    @Test
    fun `getDeltaker - har annen navBruker i kontekst, deltaker finnes - returnerer badRequest`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            navBruker = TestData.lagNavBruker(personident = "4321"),
        )
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}") { postRequest(deltakerRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `getDeltaker - deltaker er importert fra arena - returnerer importertFraArenaDto`() = testApplication {
        setUpTestApplication()
        val innsoktDatoFraArena = LocalDate.now().minusDays(5)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            navBruker = TestData.lagNavBruker(personident = "1234"),
            innsoktDatoFraArena = innsoktDatoFraArena,
        )
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}") { postRequest(deltakerRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            val responseText = bodyAsText()
            val deltakerResponse = objectMapper.readValue<DeltakerResponse>(responseText)
            deltakerResponse.importertFraArena?.innsoktDato shouldBe innsoktDatoFraArena
            deltakerResponse.vedtaksinformasjon shouldBe null
        }
        coVerify(exactly = 1) { sporbarhetsloggService.sendAuditLog(any(), any()) }
    }

    @Test
    fun `getDeltakerHistorikk - har tilgang, deltaker finnes - returnerer historikk`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker().let { TestData.leggTilHistorikk(it, 2, 2, 1) }
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        val historikk = deltaker.getDeltakerHistorikkForVisning()
        val ansatte = TestData.lagNavAnsatteForHistorikk(historikk).associateBy { it.id }
        val enheter = TestData.lagNavEnheterForHistorikk(historikk).associateBy { it.id }

        every { navAnsattService.hentAnsatteForHistorikk(historikk) } returns ansatte
        coEvery { navEnhetService.hentEnheterForHistorikk(historikk) } returns enheter
        client.get("/deltaker/${deltaker.id}/historikk") { noBodyRequest() }.apply {
            status shouldBe HttpStatusCode.OK
            val res = bodyAsText()
            val json = objectMapper.writePolymorphicListAsString(
                historikk.toResponse(
                    ansatte,
                    deltaker.deltakerliste.arrangor.getArrangorNavn(),
                    enheter,
                    deltaker.deltakerliste.oppstart,
                ),
            )
            res shouldBe json
        }
    }

    @Test
    fun `forleng - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusDays(3),
        )
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `forleng - har tilgang, ny dato tidligere enn forrige dato - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(sluttdato = forlengDeltakelseRequest.sluttdato.plusDays(5))
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `forleng - har tilgang, har sluttet for mer enn to mnd siden - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, gyldigFra = LocalDateTime.now().minusMonths(3)),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusMonths(3),
        )
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `forleng - har tilgang, ikke under oppfolging - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            navBruker = TestData.lagNavBruker(
                oppfolgingsperioder = listOf(
                    TestData.lagOppfolgingsperiode(
                        startdato = LocalDateTime.now().minusMonths(2),
                        sluttdato = LocalDateTime.now().minusDays(2),
                    ),
                ),
            ),
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            sluttdato = forlengDeltakelseRequest.sluttdato.minusDays(3),
        )
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}/forleng") { postRequest(forlengDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `avslutt - har tilgang, har deltatt - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.HAR_SLUTTET,
                avsluttDeltakelseRequest.aarsak!!.toDeltakerStatusAarsak(),
            ),
            sluttdato = avsluttDeltakelseRequest.sluttdato,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `avslutt - har tilgang, har deltatt, mangler sluttdato - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.HAR_SLUTTET,
                avsluttDeltakelseRequest.aarsak!!.toDeltakerStatusAarsak(),
            ),
            sluttdato = avsluttDeltakelseRequest.sluttdato,
        )
        val avsluttDeltakelseRequestUtenSluttdato = AvsluttDeltakelseRequest(
            aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB),
            sluttdato = null,
            harDeltatt = true,
            begrunnelse = null,
            forslagId = null,
        )
        setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequestUtenSluttdato) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `avslutt - har tilgang, har ikke deltatt - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.IKKE_AKTUELL,
                avsluttDeltakelseRequest.aarsak!!.toDeltakerStatusAarsak(),
            ),
            startdato = null,
            sluttdato = null,
        )
        val avsluttDeltakelseRequestIkkeDeltatt = AvsluttDeltakelseRequest(
            aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT),
            sluttdato = null,
            harDeltatt = false,
            begrunnelse = "begrunnelse",
            forslagId = null,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequestIkkeDeltatt) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `avslutt - har tilgang, har ikke deltatt, mer enn 15 dager siden - feiler ikke`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR, gyldigFra = LocalDateTime.now().minusDays(20)),
        )
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                DeltakerStatus.Type.IKKE_AKTUELL,
                avsluttDeltakelseRequest.aarsak!!.toDeltakerStatusAarsak(),
            ),
            startdato = null,
            sluttdato = null,
        )
        val avsluttDeltakelseRequestIkkeDeltatt = AvsluttDeltakelseRequest(
            aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT),
            sluttdato = null,
            harDeltatt = false,
            begrunnelse = null,
            forslagId = null,
        )

        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequestIkkeDeltatt) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `avslutt - har tilgang, status VENTER PA OPPSTART - feiler`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))
        setupMocks(deltaker, null)

        client.post("/deltaker/${deltaker.id}/avslutt") { postRequest(avsluttDeltakelseRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `reaktiver - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL))
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = null,
            sluttdato = null,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client
            .post("/deltaker/${deltaker.id}/reaktiver") { postRequest(reaktiverDeltakelseRequest) }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
                )
            }
    }

    @Test
    fun `reaktiver - deltaker har sluttet - returnerer bad request`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, gyldigFra = LocalDateTime.now().minusMonths(3)),
            sluttdato = LocalDate.now().minusMonths(1),
        )
        setupMocks(deltaker, null)

        client
            .post("/deltaker/${deltaker.id}/reaktiver") { postRequest(reaktiverDeltakelseRequest) }
            .apply {
                status shouldBe HttpStatusCode.BadRequest
            }
    }

    @Test
    fun `fjern oppstartsdato - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = LocalDate.now().plusWeeks(1),
            sluttdato = LocalDate.now().plusMonths(3),
        )
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = null,
            sluttdato = null,
        )
        val (ansatte, enhet) = setupMocks(deltaker, oppdatertDeltaker)

        client.post("/deltaker/${deltaker.id}/fjern-oppstartsdato") { postRequest(fjernOppstartsdatoRequest) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe objectMapper.writeValueAsString(
                oppdatertDeltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
            )
        }
    }

    @Test
    fun `avvis forslag - har tilgang - returnerer oppdatert deltaker`() = testApplication {
        setUpTestApplication()
        val deltaker = TestData.lagDeltaker()
        val forslag = TestData.lagForslag(deltakerId = deltaker.id)
        every { forslagService.get(forslag.id) } returns Result.success(forslag)

        val (ansatte, enhet) = setupMocks(deltaker, null)
        client
            .post("/forslag/${forslag.id}/avvis") { postRequest(avvisForslagRequest) }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    deltaker.toDeltakerResponse(ansatte, enhet, true, emptyList()),
                )
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
        header("aktiv-enhet", "0101")
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
                mockk(),
                forslagService,
                amtDistribusjonClient,
                sporbarhetsloggService,
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
            )
        }
    }

    private val deltakerRequest = DeltakerRequest("1234")
    private val bakgrunnsinformasjonRequest = EndreBakgrunnsinformasjonRequest("Oppdatert bakgrunnsinformasjon")
    private val innholdRequest = EndreInnholdRequest(listOf(InnholdDto("annet", "beskrivelse")))
    private val deltakelsesmengdeRequest = EndreDeltakelsesmengdeRequest(
        deltakelsesprosent = 50,
        dagerPerUke = 3,
        begrunnelse = "begrunnelse",
        gyldigFra = LocalDate.now(),
        forslagId = null,
    )
    private val startdatoRequest =
        EndreStartdatoRequest(LocalDate.now().plusWeeks(1), sluttdato = LocalDate.now().plusMonths(2), "begrunnelse", null)
    private val ikkeAktuellRequest = IkkeAktuellRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB), "begrunnelse", null)
    private val reaktiverDeltakelseRequest = ReaktiverDeltakelseRequest("begrunnelse")
    private val forlengDeltakelseRequest = ForlengDeltakelseRequest(LocalDate.now().plusWeeks(3), "begrunnelse", null)
    private val avsluttDeltakelseRequest =
        AvsluttDeltakelseRequest(
            DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB),
            LocalDate.now(),
            harDeltatt = true,
            harFullfort = null,
            "begrunnelse",
            null,
        )
    private val sluttdatoRequest = EndreSluttdatoRequest(LocalDate.now().minusDays(1), "begrunnelse", null)
    private val sluttarsakRequest =
        EndreSluttarsakRequest(DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT), "begrunnelse", null)
    private val avvisForslagRequest = AvvisForslagRequest("Avvist fordi..")
    private val fjernOppstartsdatoRequest = FjernOppstartsdatoRequest("begrunnelse", null)

    private fun setupMocks(
        deltaker: Deltaker,
        oppdatertDeltaker: Deltaker?,
        forslag: List<Forslag> = emptyList(),
    ): Pair<Map<UUID, NavAnsatt>, NavEnhet?> {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        every { deltakerService.getDeltakelser(deltaker.navBruker.personident, deltaker.deltakerliste.id) } returns listOf(deltaker)
        coEvery { amtDistribusjonClient.digitalBruker(any()) } returns true
        every { forslagService.getForDeltaker(deltaker.id) } returns forslag
        every { unleashToggle.erKometMasterForTiltakstype(Tiltakstype.ArenaKode.ARBFORB) } returns true

        return if (oppdatertDeltaker != null) {
            coEvery {
                deltakerService.oppdaterDeltaker(deltaker, any(), any(), any())
            } returns oppdatertDeltaker

            mockAnsatteOgEnhetForDeltaker(oppdatertDeltaker)
        } else {
            mockAnsatteOgEnhetForDeltaker(deltaker)
        }
    }

    private fun mockAnsatteOgEnhetForDeltaker(deltaker: Deltaker): Pair<Map<UUID, NavAnsatt>, NavEnhet?> {
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.id }
        val enhet = deltaker.vedtaksinformasjon?.let { TestData.lagNavEnhet(id = it.sistEndretAvEnhet) }
        val enheter = TestData.lagNavEnheterForHistorikk(deltaker.historikk).associateBy { it.id }

        every { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        enhet?.let { every { navEnhetService.hentEnhet(it.id) } returns it }
        coEvery { navEnhetService.hentEnheterForHistorikk(any()) } returns enheter

        return Pair(ansatte, enhet)
    }
}
