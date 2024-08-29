package no.nav.amt.deltaker.bff.deltaker.forslag

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingProducer
import no.nav.amt.deltaker.bff.kafka.utils.assertProduced
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class ForslagServiceTest {
    companion object {
        lateinit var repository: ForslagRepository
        lateinit var service: ForslagService
        val navEnhetService = mockk<NavEnhetService>()
        val navAnsattService = mockk<NavAnsattService>()
        val arrangorMeldingProducer = ArrangorMeldingProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost()))

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
            repository = ForslagRepository()
            service = ForslagService(repository, navAnsattService, navEnhetService, arrangorMeldingProducer)
        }
    }

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `avvisForslag - produserer avvist forslag og sletter i db`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val navAnsatt = TestData.lagNavAnsatt()
        val navEnhet = TestData.lagNavEnhet()
        coEvery { navAnsattService.hentEllerOpprettNavAnsatt(navAnsatt.navIdent) } returns navAnsatt
        coEvery { navEnhetService.hentOpprettEllerOppdaterNavEnhet(navEnhet.enhetsnummer) } returns navEnhet
        val opprinneligForslag = TestData.lagForslag(deltakerId = deltaker.id)
        repository.upsert(opprinneligForslag)
        val begrunnelseAvslag = "Avslått fordi.."

        service.avvisForslag(opprinneligForslag, begrunnelseAvslag, navAnsatt.navIdent, navEnhet.enhetsnummer)

        repository.get(opprinneligForslag.id).getOrNull() shouldBe null

        assertProduced(
            opprinneligForslag.copy(
                status = Forslag.Status.Avvist(Forslag.NavAnsatt(navAnsatt.id, navEnhet.id), LocalDateTime.now(), begrunnelseAvslag),
            ),
        )
    }
}
