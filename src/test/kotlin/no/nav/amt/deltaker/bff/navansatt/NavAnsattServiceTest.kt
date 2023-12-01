package no.nav.amt.deltaker.bff.navansatt

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.mockHttpClient
import org.junit.BeforeClass
import org.junit.Test

class NavAnsattServiceTest {
    companion object {
        lateinit var repository: NavAnsattRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = NavAnsattRepository()
        }
    }

    @Test
    fun `hentNavAnsatt - navansatt finnes i db - henter fra db`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val navAnsattService = NavAnsattService(repository, mockk<AmtPersonServiceClient>())

        runBlocking {
            val navAnsattFraDb = navAnsattService.hentNavAnsatt(navAnsatt.navident)
            navAnsattFraDb shouldBe navAnsatt
        }
    }

    @Test
    fun `hentNavAnsatt - navansatt finnes ikke i db - henter fra personservice og lagrer`() {
        val navAnsattResponse = TestData.lagNavAnsatt()
        val httpClient = mockHttpClient(objectMapper.writeValueAsString(navAnsattResponse))
        val amtPersonServiceClient = AmtPersonServiceClient(
            baseUrl = "http://amt-person-service",
            scope = "scope",
            httpClient = httpClient,
            azureAdTokenClient = mockAzureAdClient(),
        )
        val navAnsattService = NavAnsattService(repository, amtPersonServiceClient)

        runBlocking {
            val navAnsatt = navAnsattService.hentNavAnsatt(navAnsattResponse.navident)

            navAnsatt shouldBe navAnsattResponse
            repository.get(navAnsattResponse.id) shouldBe navAnsattResponse
        }
    }

    @Test
    fun `oppdaterNavAnsatt - navansatt finnes - blir oppdatert`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val oppdatertNavAnsatt = navAnsatt.copy(navn = "Nytt Navn")
        val navAnsattService = NavAnsattService(repository, mockk<AmtPersonServiceClient>())

        navAnsattService.oppdaterNavAnsatt(oppdatertNavAnsatt)

        repository.get(navAnsatt.id) shouldBe oppdatertNavAnsatt
    }

    @Test
    fun `slettNavAnsatt - navansatt blir slettet`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val navAnsattService = NavAnsattService(repository, mockk<AmtPersonServiceClient>())

        navAnsattService.slettNavAnsatt(navAnsatt.id)

        repository.get(navAnsatt.id) shouldBe null
    }
}
