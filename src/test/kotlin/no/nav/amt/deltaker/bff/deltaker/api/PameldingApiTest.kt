package no.nav.amt.deltaker.bff.deltaker.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
import no.nav.amt.deltaker.bff.deltaker.api.model.AvbrytUtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.KladdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.utils.noBodyRequest
import no.nav.amt.deltaker.bff.deltaker.api.utils.postRequest
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.util.UUID

class PameldingApiTest {
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
        client.post("/pamelding") { postRequest(pameldingRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}") { postRequest(utkastRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}/kladd") { postRequest(kladdRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }.status shouldBe HttpStatusCode.Forbidden
        client.delete("/pamelding/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
        client.post("/pamelding/${UUID.randomUUID()}/avbryt") { postRequest(avbrytUtkastRequest) }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste autorisering - mangler token - returnerer 401`() = testApplication {
        setUpTestApplication()
        client.post("/pamelding") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}/kladd") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
        client.delete("/pamelding/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
        client.post("/pamelding/${UUID.randomUUID()}/avbryt") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `post pamelding - har tilgang - returnerer deltaker`() = testApplication {
        val deltaker = TestData.lagDeltaker()
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.navIdent }
        val navEnhet = TestData.lagNavEnhet(enhetsnummer = deltaker.vedtaksinformasjon!!.sistEndretAvEnhet!!)

        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        coEvery { pameldingService.opprettKladd(any(), any(), any(), any()) } returns deltaker
        coEvery { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        coEvery { navEnhetService.hentEnhet(navEnhet.enhetsnummer) } returns navEnhet

        setUpTestApplication()

        client.post("/pamelding") { postRequest(pameldingRequest) }.apply {
            TestCase.assertEquals(HttpStatusCode.OK, status)
            TestCase.assertEquals(
                objectMapper.writeValueAsString(deltaker.toDeltakerResponse(ansatte, navEnhet)),
                bodyAsText(),
            )
        }
    }

    @Test
    fun `post pamelding - deltakerliste finnes ikke - reurnerer 404`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        coEvery {
            pameldingService.opprettKladd(
                any(),
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
    fun `kladd deltakerId - har tilgang - returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.KLADD))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        coEvery { pameldingService.upsertKladd(any()) } returns Unit

        setUpTestApplication()
        client.post("/pamelding/${deltaker.id}/kladd") { postRequest(kladdRequest) }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `kladd deltakerId - har tilgang, feil deltakerstatus - returnerer 400`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        coEvery { pameldingService.upsertKladd(any()) } throws IllegalArgumentException()

        setUpTestApplication()
        client.post("/pamelding/${deltaker.id}/kladd") { postRequest(kladdRequest) }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `pamelding deltakerId - har tilgang - oppretter utkast og returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker()
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        coEvery { pameldingService.upsertUtkast(any()) } returns Unit

        setUpTestApplication()
        client.post("/pamelding/${deltaker.id}") { postRequest(utkastRequest) }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `pamelding deltakerId - deltaker finnes ikke - returnerer 404`() = testApplication {
        every { deltakerService.get(any()) } throws NoSuchElementException()

        setUpTestApplication()
        client.post("/pamelding/${UUID.randomUUID()}") { postRequest(utkastRequest) }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `pamelding deltakerId uten godkjenning - har tilgang - oppretter ferdig godkjent deltaker og returnerer 200`() =
        testApplication {
            coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
            val deltaker = TestData.lagDeltaker()
            every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
            coEvery { pameldingService.meldPaUtenGodkjenning(any()) } returns Unit

            setUpTestApplication()
            client.post("/pamelding/${deltaker.id}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }
                .apply {
                    status shouldBe HttpStatusCode.OK
                }
        }

    @Test
    fun `pamelding deltakerId uten godkjenning - deltaker finnes ikke - returnerer 404`() = testApplication {
        every { deltakerService.get(any()) } throws NoSuchElementException()

        setUpTestApplication()
        client.post("/pamelding/${UUID.randomUUID()}/utenGodkjenning") { postRequest(pameldingUtenGodkjenningRequest) }
            .apply {
                status shouldBe HttpStatusCode.NotFound
            }
    }

    @Test
    fun `slett kladd - har tilgang, deltaker er KLADD - sletter deltaker og returnerer 200`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        every { pameldingService.slettKladd(deltaker) } returns true

        setUpTestApplication()
        client.delete("/pamelding/${deltaker.id}") { noBodyRequest() }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `slett kladd - deltaker har ikke status KLADD - returnerer 400`() = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING))
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
        every { pameldingService.slettKladd(deltaker) } returns false

        setUpTestApplication()
        client.delete("/pamelding/${deltaker.id}") { noBodyRequest() }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `avbryt utkast - har tilgang, deltakerstatus er UTKAST_TIL_PAMELDING - avbryter utkast og returnerer 200`() =
        testApplication {
            coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
            val deltaker =
                TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING))
            every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
            coEvery { pameldingService.avbrytUtkast(deltaker, any(), any(), any()) } returns Unit

            setUpTestApplication()
            client.post("/pamelding/${deltaker.id}/avbryt") { postRequest(avbrytUtkastRequest) }.apply {
                status shouldBe HttpStatusCode.OK
            }
        }

    @Test
    fun `avbryt utkast - har tilgang, deltakerstatus er ikke UTKAST_TIL_PAMELDING - returnerer 400`() =
        testApplication {
            coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
            val deltaker =
                TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VURDERES))
            every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)
            coEvery {
                pameldingService.avbrytUtkast(
                    deltaker,
                    any(),
                    any(),
                    any(),
                )
            } throws IllegalArgumentException("Kan ikke avbryte utkast for deltaker med id 123")

            setUpTestApplication()
            client.post("/pamelding/${deltaker.id}/avbryt") { postRequest(avbrytUtkastRequest) }.apply {
                status shouldBe HttpStatusCode.BadRequest
            }
        }

    @Test
    fun `avbryt utkast - deltaker finnes ikke - returnerer 404`() = testApplication {
        every { deltakerService.get(any()) } throws NoSuchElementException()

        setUpTestApplication()
        client.post("/pamelding/${UUID.randomUUID()}/avbryt") { postRequest(avbrytUtkastRequest) }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
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

    private val utkastRequest = UtkastRequest(emptyList(), "Bakgrunnen for...", null, null)
    private val avbrytUtkastRequest =
        AvbrytUtkastRequest(DeltakerStatus.Aarsak(DeltakerStatus.Aarsak.Type.FATT_JOBB, null))
    private val kladdRequest = KladdRequest(emptyList(), "Bakgrunnen for...", null, null)
    private val pameldingRequest = PameldingRequest(UUID.randomUUID(), "1234")
    private val pameldingUtenGodkjenningRequest = PameldingUtenGodkjenningRequest(
        emptyList(),
        "Bakgrunnen for...",
        null,
        null,
    )
}
