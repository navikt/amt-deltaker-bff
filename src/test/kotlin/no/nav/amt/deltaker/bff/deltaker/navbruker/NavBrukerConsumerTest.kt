package no.nav.amt.deltaker.bff.deltaker.navbruker

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.NavBrukerDto
import no.nav.amt.deltaker.bff.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.toDto
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NavBrukerConsumerTest {
    companion object {
        private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
        private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
            forslagService = mockk(relaxed = true),
        )
        private val navBrukerService: NavBrukerService =
            NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService)

        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            navBrukerService = navBrukerService,
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
        )

        @JvmStatic
        @BeforeAll
        fun setup() {
            SingletonPostgres16Container
        }
    }

    @Test
    fun `consumeNavBruker - ny navBruker - upserter`() {
        val navBruker = TestData.lagNavBruker()
        val navVeileder = TestData.lagNavAnsatt(navBruker.navVeilederId!!)
        val navEnhet = TestData.lagNavEnhet(navBruker.navEnhetId!!)
        val navBrukerConsumer = NavBrukerConsumer(navBrukerService, pameldingService)

        MockResponseHandler.addNavAnsattResponse(navVeileder)
        MockResponseHandler.addNavEnhetGetResponse(navEnhet)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, navBruker.toDto(navEnhet).toJSON())
        }

        navBrukerService.get(navBruker.personId).getOrNull() shouldBe navBruker
    }

    @Test
    fun `consumeNavBruker - oppdatert navBruker - upserter`() {
        val navBruker = TestData.lagNavBruker()
        val navEnhet = TestData.lagNavEnhet(navBruker.navEnhetId!!)
        TestRepository.insert(navEnhet)
        TestRepository.insert(navBruker)

        val oppdatertNavBruker = navBruker.copy(fornavn = "Oppdatert NavBruker")

        val navBrukerConsumer = NavBrukerConsumer(navBrukerService, pameldingService)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, oppdatertNavBruker.toDto(navEnhet).toJSON())
        }

        navBrukerService.get(navBruker.personId).getOrNull() shouldBe oppdatertNavBruker
    }

    @Test
    fun `consumeNavBruker - avsluttet oppfolging - sletter kladd`() {
        val navBruker = TestData.lagNavBruker()
        val navEnhet = TestData.lagNavEnhet(navBruker.navEnhetId!!)
        val kladd = TestData.lagDeltakerKladd(navBruker = navBruker)
        TestRepository.insert(navEnhet)
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

        val navBrukerConsumer = NavBrukerConsumer(navBrukerService, pameldingService)

        runBlocking {
            navBrukerConsumer.consume(navBruker.personId, oppdatertNavBruker.toDto(navEnhet).toJSON())
        }

        navBrukerService.get(navBruker.personId).getOrNull() shouldBe oppdatertNavBruker
        deltakerService.get(kladd.id).getOrNull() shouldBe null
    }
}

fun NavBruker.toDto(navEnhet: NavEnhet) = NavBrukerDto(
    personId,
    personident,
    fornavn,
    mellomnavn,
    etternavn,
    adressebeskyttelse,
    oppfolgingsperioder,
    innsatsgruppe,
    adresse,
    erSkjermet,
    navEnhet.toDto(),
    navVeilederId,
)

fun NavBrukerDto.toJSON(): String = objectMapper.writeValueAsString(this)
