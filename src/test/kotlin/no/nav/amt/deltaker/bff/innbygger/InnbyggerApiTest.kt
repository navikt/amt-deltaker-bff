package no.nav.amt.deltaker.bff.innbygger

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.innbygger.model.toInnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.configureEnvForAuthentication
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.tokenXToken
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Before
import org.junit.Test
import java.util.UUID

class InnbyggerApiTest {
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
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))
        coEvery { deltakerService.get(any()) } returns Result.success(TestData.lagDeltaker())

        setUpTestApplication()
        client.get("/innbygger/${UUID.randomUUID()}") { noBodyRequest() }.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `skal teste tilgangskontroll - mangler token - returnerer 401`() = testApplication {
        coEvery { deltakerService.get(any()) } returns Result.success(TestData.lagDeltaker())

        setUpTestApplication()
        client.get("/innbygger/${UUID.randomUUID()}").status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `get id - innbygger har tilgang - returnerer 200 og deltaker`() {
        val deltaker = TestData.lagDeltaker()

        mockTestApi(deltaker) { client, ansatte, enhet ->
            val res = client.get("innbygger/${deltaker.id}") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.OK
            res.bodyAsText() shouldBe objectMapper.writeValueAsString(deltaker.toInnbyggerDeltakerResponse(ansatte, enhet))
        }
    }

    @Test
    fun `get id - deltaker finnes ikke - returnerer 404`() {
        coEvery { deltakerService.get(any()) } returns Result.failure(NoSuchElementException())

        testApplication {
            setUpTestApplication()
            val res = client.get("innbygger/${UUID.randomUUID()}") { noBodyRequest() }
            res.status shouldBe HttpStatusCode.NotFound
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
            )
        }
    }

    private fun mockTestApi(
        deltaker: Deltaker,
        block: suspend (client: HttpClient, ansatte: Map<UUID, NavAnsatt>, enhet: NavEnhet?) -> Unit,
    ) = testApplication {
        coEvery { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)
        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        val (ansatte, enhet) = mockAnsatteOgEnhetForDeltaker(deltaker)

        setUpTestApplication()
        block(client, ansatte, enhet)
    }

    private fun mockAnsatteOgEnhetForDeltaker(deltaker: Deltaker): Pair<Map<UUID, NavAnsatt>, NavEnhet?> {
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker).associateBy { it.id }
        val enhet = deltaker.vedtaksinformasjon?.let { TestData.lagNavEnhet(id = it.sistEndretAvEnhet) }

        every { navAnsattService.hentAnsatteForDeltaker(deltaker) } returns ansatte
        enhet?.let { every { navEnhetService.hentEnhet(it.id) } returns it }

        return Pair(ansatte, enhet)
    }
}
