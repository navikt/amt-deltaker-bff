package no.nav.amt.deltaker.bff.deltaker.navbruker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class NavBrukerConsumerTest {
    companion object {
        private val repository: NavBrukerRepository = NavBrukerRepository()
        private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
        )

        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), repository),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
        }
    }

    @Test
    fun `consumeNavBruker - ny navBruker - upserter`() {
        val navBruker = TestData.lagNavBruker()
        val navBrukerConsumer = NavBrukerConsumer(repository, pameldingService)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, objectMapper.writeValueAsString(navBruker))
        }

        repository.get(navBruker.personId).getOrNull() shouldBe navBruker
    }

    @Test
    fun `consumeNavBruker - oppdatert navBruker - upserter`() {
        val navBruker = TestData.lagNavBruker()
        repository.upsert(navBruker)

        val oppdatertNavBruker = navBruker.copy(fornavn = "Oppdatert NavBruker")

        val navBrukerConsumer = NavBrukerConsumer(repository, pameldingService)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, objectMapper.writeValueAsString(oppdatertNavBruker))
        }

        repository.get(navBruker.personId).getOrNull() shouldBe oppdatertNavBruker
    }

    @Test
    fun `consumeNavBruker - avsluttet oppfolging - sletter kladd`() {
        val navBruker = TestData.lagNavBruker()
        repository.upsert(navBruker)
        val kladd = TestData.lagDeltakerKladd(navBruker = navBruker)
        TestRepository.insert(kladd)
        MockResponseHandler.addSlettKladdResponse(kladd.id)

        val oppdatertNavBruker = navBruker.copy(
            innsatsgruppe = null,
            oppfolgingsperioder = listOf(
                TestData.lagOppfolgingsperiode(
                    startdato = LocalDateTime.now().minusYears(1),
                    sluttdato = LocalDateTime.now().minusDays(2),
                ),
            ),
        )

        val navBrukerConsumer = NavBrukerConsumer(repository, pameldingService)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, objectMapper.writeValueAsString(oppdatertNavBruker))
        }

        repository.get(navBruker.personId).getOrNull() shouldBe oppdatertNavBruker
        deltakerService.get(kladd.id).getOrNull() shouldBe null
    }
}
