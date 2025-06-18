package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
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
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyRequest
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyTiltakskoordinatorRequest
import no.nav.amt.deltaker.bff.deltaker.api.utils.postRequest
import no.nav.amt.deltaker.bff.deltaker.api.utils.postTiltakskoordinatorRequest
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteStengtException
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.toDeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.toResponse
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TiltakskoordinatorDeltakerlisteApiTest {
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val deltakerlisteService = mockk<DeltakerlisteService>()
    private val tiltakskoordinatorService = mockk<TiltakskoordinatorService>()
    private val navEnhetService = mockk<NavEnhetService>()

    @Before
    fun setup() {
        configureEnvForAuthentication()
    }

    @Test
    fun `skal teste autentisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere").status shouldBe HttpStatusCode.Unauthorized
        client.post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/tilgang/legg-til").status shouldBe HttpStatusCode.Unauthorized
        client.post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere/del-med-arrangor").status shouldBe
            HttpStatusCode.Unauthorized
        client.post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere/sett-paa-venteliste").status shouldBe
            HttpStatusCode.Unauthorized
        client.post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere/gi-avslag").status shouldBe
            HttpStatusCode.Unauthorized
    }

    @Test
    fun `skal teste autentisering - mangler AD rolle - returnerer 401`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.hentMedFellesOppstart(deltakerliste.id) } returns Result.success(deltakerliste)

        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/tilgang/legg-til") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere/del-med-arrangor") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }

        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere/sett-paa-venteliste") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere/gi-avslag") { noBodyRequest() }
            .apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
    }

    @Test
    fun `get deltakerliste - liste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        every { deltakerlisteService.hentMedFellesOppstart(any()) } returns Result.failure(NoSuchElementException())

        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}") {
                noBodyTiltakskoordinatorRequest()
            }.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `get deltakerliste - liste finnes - returnerer 200 og liste`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.hentMedFellesOppstart(deltakerliste.id) } returns Result.success(deltakerliste)
        every { tiltakskoordinatorService.hentKoordinatorer(any()) } returns emptyList()
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(deltakerliste.toResponse(emptyList()))
            }
    }

    @Test
    fun `get deltakere - mangler tilgang til deltakerliste - returnerer 403`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id) } returns deltakerliste
        coEvery { tilgangskontrollService.verifiserTiltakskoordinatorTilgang(any(), any()) } throws AuthorizationException("")
        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.Forbidden
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(any()) } throws NoSuchElementException()
        coEvery { tiltakskoordinatorService.hentDeltakereForDeltakerliste(any()) } returns emptyList()
        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.NotFound
            }
    }

    @Test
    fun `get deltakere - deltakerliste er stengt - returnerer 410`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(any()) } throws DeltakerlisteStengtException()
        client
            .get("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.Gone
            }
    }

    @Test
    fun `get deltakere - deltakerliste finnes - returnerer liste med deltakere`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        val deltakerliste = TestData.lagDeltakerliste()
        val deltakere = (0..5).map { TestData.lagTiltakskoordinatorDeltaker(deltakerliste = deltakerliste) }
        val navEnheter = deltakere
            .mapNotNull { it.navBruker.navEnhetId }
            .distinct()
            .map { TestData.lagNavEnhet(it) }
            .associateBy { it.id }

        every { navEnhetService.hentEnheter(any()) } returns navEnheter
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id) } returns deltakerliste
        coEvery { tiltakskoordinatorService.hentDeltakereForDeltakerliste(deltakerliste.id) } returns deltakere
        deltakere.forEach {
            every { tilgangskontrollService.harKoordinatorTilgangTilPerson(any(), it.navBruker) } returns true
        }

        client
            .get("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere") { noBodyTiltakskoordinatorRequest() }
            .apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe objectMapper.writeValueAsString(
                    deltakere.map { deltaker ->
                        deltaker.toDeltakerResponse(true)
                    },
                )
            }
    }

    @Test
    fun `legg til tilgang - har ikke tilgang fra før - returnerer 200`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        coEvery {
            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(
                any(),
                deltakerliste.id,
            )
        } returns Result.success(TestData.lagTiltakskoordinatorTilgang())

        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/tilgang/legg-til") { noBodyTiltakskoordinatorRequest() }
            .status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `legg til tilgang - har tilgang fra før - returnerer 400`() = testApplication {
        setUpTestApplication()
        coEvery {
            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(
                any(),
                any(),
            )
        } returns Result.failure(IllegalArgumentException())

        client
            .post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/tilgang/legg-til") { noBodyTiltakskoordinatorRequest() }
            .status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `post del-med-arrangor - mangler tilgang til deltakerliste - returnerer 403`() = testApplication {
        setUpTestApplication()
        val deltakerliste = TestData.lagDeltakerliste()
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id) } returns deltakerliste
        coEvery { tilgangskontrollService.tilgangTilDeltakereGuard(any(), any(), any()) } throws AuthorizationException("")

        client
            .post("/tiltakskoordinator/deltakerliste/${deltakerliste.id}/deltakere/del-med-arrangor") {
                postTiltakskoordinatorRequest(listOf(UUID.randomUUID()))
            }.apply {
                status shouldBe HttpStatusCode.Forbidden
            }
    }

    @Test
    fun `post del-med-arrangor - deltakerliste finnes ikke - returnerer 404`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        coEvery { tiltakskoordinatorService.hentDeltakereForDeltakerliste(any()) } returns emptyList()
        coEvery { tilgangskontrollService.tilgangTilDeltakereGuard(any(), any(), any()) } throws NoSuchElementException()
        client
            .post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere/del-med-arrangor") {
                postTiltakskoordinatorRequest(listOf(UUID.randomUUID()))
            }.apply {
                status shouldBe HttpStatusCode.NotFound
            }
    }

    @Test
    fun `post sett-paa-venteliste - deltakerliste er feil type - returnerer unauthorized`() = testApplication {
        setUpTestApplication()
        mockTilgangTilDeltakerliste()
        coEvery { tilgangskontrollService.verifiserTiltakskoordinatorTilgang(any(), any()) } returns Unit
        every { deltakerlisteService.verifiserTilgjengeligDeltakerliste(any()) } throws NoSuchElementException()
        client
            .post("/tiltakskoordinator/deltakerliste/${UUID.randomUUID()}/deltakere/del-med-arrangor") {
                postRequest(listOf(UUID.randomUUID()))
            }.apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
    }

    /*
        @Test
        fun `sett-paa-venteliste - deltakere i feil liste - returnerer 401`() = testApplication {
            val deltaker1 = TestData.lagDeltaker()
            val deltaker2 = TestData.lagDeltaker(deltakerliste = TestData.lagDeltakerliste(id = UUID.randomUUID()))
            coEvery { deltakerService.getDeltakelser(any()) } returns listOf(deltaker1, deltaker2)
            coEvery { unleashToggle.erKometMasterForTiltakstype(deltaker1.deltakerliste.tiltakstype.arenaKode) } returns true

            val request = DeltakereRequest(
                deltakere = listOf(deltaker1.id, deltaker2.id),
                deltakerlisteId = deltaker1.deltakerliste.id,
                endretAv = "Nav Veiledersen"
            )
            setUpTestApplication()
            client.post("$apiPath/sett-paa-venteliste") { postRequest(request) }.apply {
                status shouldBe HttpStatusCode.Forbidden
                bodyAsText() shouldBe ""
            }
        }
     */
    private fun mockTilgangTilDeltakerliste() {
        coEvery { tilgangskontrollService.verifiserTiltakskoordinatorTilgang(any(), any()) } returns Unit
    }

    private fun ApplicationTestBuilder.setUpTestApplication() {
        application {
            configureSerialization()
            configureAuthentication(Environment())
            configureRouting(
                tilgangskontrollService,
                mockk(),
                mockk(),
                mockk(),
                navEnhetService,
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                deltakerlisteService,
                mockk(),
                tiltakskoordinatorService,
            )
        }
    }
}
