package no.nav.amt.deltaker.bff.navansatt.navenhet

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavEnhetDto
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.mockHttpClient
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class NavEnhetServiceTest {
    companion object {
        lateinit var repository: NavEnhetRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
            repository = NavEnhetRepository()
        }
    }

    @Test
    fun `hentOpprettEllerOppdaterNavEnhet - navenhet finnes i db - henter fra db`() {
        val navEnhet = TestData.lagNavEnhet()
        repository.upsert(navEnhet)
        val navEnhetService = NavEnhetService(repository, mockk())

        runBlocking {
            val navEnhetFraDb = navEnhetService.hentOpprettEllerOppdaterNavEnhet(navEnhet.enhetsnummer)
            navEnhetFraDb shouldBe navEnhet
        }
    }

    @Test
    fun `hentOpprettEllerOppdaterNavEnhet - navenhet finnes ikke i db - henter fra personservice og lagrer`() {
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
            val navEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(navEnhetResponse.enhetsnummer)

            navEnhet shouldBe navEnhetResponse
            repository.get(navEnhetResponse.enhetsnummer)?.toNavEnhet() shouldBe navEnhetResponse
        }
    }

    @Test
    fun `hentOpprettEllerOppdaterNavEnhet - utdatert navenhet finnes i db - henter fra personservice og oppdaterer`() {
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
            val navEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(navEnhetResponse.enhetsnummer)

            navEnhet shouldBe navEnhetResponse
            repository.get(navEnhetResponse.enhetsnummer)?.toNavEnhet() shouldBe navEnhetResponse
        }
    }

    @Test
    fun `hentEnheterForHistorikk - historikk endret av flere ansatte - returnerer alle enheter`() {
        val navEnhetService = NavEnhetService(repository, mockk())
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

        val enheter = TestData.lagNavEnheterForHistorikk(historikk)

        enheter.forEach { TestRepository.insert(it) }
        TestRepository.insert(deltaker)

        val faktiskeEnheter = navEnhetService.hentEnheterForHistorikk(historikk)
        faktiskeEnheter.size shouldBe enheter.size

        faktiskeEnheter.toList().map { it.second }.containsAll(enheter) shouldBe true
    }
}
