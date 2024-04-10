package no.nav.amt.deltaker.bff.navansatt.navenhet

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavEnhetDto
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.mockHttpClient
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class NavEnhetServiceTest {
    companion object {
        lateinit var repository: NavEnhetRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = NavEnhetRepository()
        }
    }

    @Test
    fun `opprettEllerOppdaterNavEnhet - navenhet finnes i db - henter fra db`() {
        val navEnhet = TestData.lagNavEnhet()
        repository.upsert(navEnhet)
        val navEnhetService = NavEnhetService(repository, mockk())

        runBlocking {
            val navEnhetFraDb = navEnhetService.opprettEllerOppdaterNavEnhet(navEnhet.enhetsnummer)
            navEnhetFraDb shouldBe navEnhet
        }
    }

    @Test
    fun `opprettEllerOppdaterNavEnhet - navenhet finnes ikke i db - henter fra personservice og lagrer`() {
        val navEnhetResponse = TestData.lagNavEnhet()
        val httpClient =
            mockHttpClient(
                objectMapper.writeValueAsString(NavEnhetDto(navEnhetResponse.id, navEnhetResponse.enhetsnummer, navEnhetResponse.navn)),
            )
        val amtPersonServiceClient = AmtPersonServiceClient(
            baseUrl = "http://amt-person-service",
            scope = "scope",
            httpClient = httpClient,
            azureAdTokenClient = mockAzureAdClient(),
        )
        val navEnhetService = NavEnhetService(repository, amtPersonServiceClient)

        runBlocking {
            val navEnhet = navEnhetService.opprettEllerOppdaterNavEnhet(navEnhetResponse.enhetsnummer)

            navEnhet shouldBe navEnhetResponse
            repository.get(navEnhetResponse.enhetsnummer)?.toNavEnhet() shouldBe navEnhetResponse
        }
    }

    @Test
    fun `opprettEllerOppdaterNavEnhet - utdatert navenhet finnes i db - henter fra personservice og oppdaterer`() {
        val utdatertNavEnhet = TestData.lagNavEnhet()
        TestRepository.insert(
            NavEnhetDbo(
                id = utdatertNavEnhet.id,
                enhetsnummer = utdatertNavEnhet.enhetsnummer,
                navn = utdatertNavEnhet.navn,
                sistEndret = LocalDateTime.now().minusMonths(2),
            ),
        )
        val navEnhetResponse = TestData.lagNavEnhet(id = utdatertNavEnhet.id, utdatertNavEnhet.enhetsnummer, "Oppdatert navn")
        val httpClient =
            mockHttpClient(
                objectMapper.writeValueAsString(NavEnhetDto(navEnhetResponse.id, navEnhetResponse.enhetsnummer, navEnhetResponse.navn)),
            )
        val amtPersonServiceClient = AmtPersonServiceClient(
            baseUrl = "http://amt-person-service",
            scope = "scope",
            httpClient = httpClient,
            azureAdTokenClient = mockAzureAdClient(),
        )
        val navEnhetService = NavEnhetService(repository, amtPersonServiceClient)

        runBlocking {
            val navEnhet = navEnhetService.opprettEllerOppdaterNavEnhet(navEnhetResponse.enhetsnummer)

            navEnhet shouldBe navEnhetResponse
            repository.get(navEnhetResponse.enhetsnummer)?.toNavEnhet() shouldBe navEnhetResponse
        }
    }
}
