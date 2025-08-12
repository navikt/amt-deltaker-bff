package no.nav.amt.deltaker.bff.navansatt

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.mockHttpClient
import no.nav.amt.lib.ktor.clients.AmtPersonServiceClient
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NavAnsattServiceTest {
    companion object {
        lateinit var repository: NavAnsattRepository
        lateinit var service: NavAnsattService

        @JvmStatic
        @BeforeAll
        fun setup() {
            SingletonPostgres16Container
            repository = NavAnsattRepository()
            service = NavAnsattService(repository, mockk())
        }
    }

    @Test
    fun `hentEllerOpprettNavAnsatt - navansatt finnes i db - henter fra db`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)

        runBlocking {
            val navAnsattFraDb = service.hentEllerOpprettNavAnsatt(navAnsatt.navIdent)
            navAnsattFraDb shouldBe navAnsatt
        }
    }

    @Test
    fun `hentEllerOpprettNavAnsatt - navansatt finnes ikke i db - henter fra personservice og lagrer`() {
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
            val navAnsatt = navAnsattService.hentEllerOpprettNavAnsatt(navAnsattResponse.navIdent)

            navAnsatt shouldBe navAnsattResponse
            repository.get(navAnsattResponse.id) shouldBe navAnsattResponse
        }
    }

    @Test
    fun `oppdaterNavAnsatt - navansatt finnes - blir oppdatert`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val oppdatertNavAnsatt = navAnsatt.copy(navn = "Nytt Navn")

        service.oppdaterNavAnsatt(oppdatertNavAnsatt)

        repository.get(navAnsatt.id) shouldBe oppdatertNavAnsatt
    }

    @Test
    fun `slettNavAnsatt - navansatt blir slettet`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)

        service.slettNavAnsatt(navAnsatt.id)

        repository.get(navAnsatt.id) shouldBe null
    }

    @Test
    fun `hentAnsatteForDeltaker - deltaker endret av flere ansatte - returnerer alle ansatte`() {
        val deltaker = TestData.lagDeltaker()
        val ansatte = TestData.lagNavAnsatteForDeltaker(deltaker)

        ansatte.forEach { TestRepository.insert(it) }
        TestRepository.insert(deltaker)

        val faktiskeAnsatte = service.hentAnsatteForDeltaker(deltaker)
        faktiskeAnsatte.size shouldBe ansatte.size

        faktiskeAnsatte.toList().map { it.second }.containsAll(ansatte) shouldBe true
    }

    @Test
    fun `hentAnsatteForHistorikk - historikk endret av flere ansatte - returnerer alle ansatte`() {
        val deltaker = TestData.lagDeltaker()
        val vedtak = TestData.lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = LocalDateTime.now(),
            fattetAvNav = true,
        )
        val deltakerEndring = TestData.lagDeltakerEndring(deltakerId = deltaker.id)
        val forslag = TestData.lagForslag(
            deltakerId = deltaker.id,
            status = Forslag.Status.Avvist(
                avvistAv = Forslag.NavAnsatt(UUID.randomUUID(), UUID.randomUUID()),
                avvist = LocalDateTime.now(),
                begrunnelseFraNav = "Begrunnelse",
            ),
        )

        val historikk = listOf(
            DeltakerHistorikk.Endring(deltakerEndring),
            DeltakerHistorikk.Vedtak(vedtak),
            DeltakerHistorikk.Forslag(forslag),
        )

        val ansatte = TestData.lagNavAnsatteForHistorikk(historikk)

        ansatte.forEach { TestRepository.insert(it) }
        TestRepository.insert(deltaker)

        val faktiskeAnsatte = service.hentAnsatteForHistorikk(historikk)
        faktiskeAnsatte.size shouldBe ansatte.size

        faktiskeAnsatte.toList().map { it.second }.containsAll(ansatte) shouldBe true
    }
}
