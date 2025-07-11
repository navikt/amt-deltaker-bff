package no.nav.amt.deltaker.bff.testdata

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingProducer
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.toInnhold
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestdataServiceTest {
    companion object {
        private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
        private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
            forslagService = mockk(),
        )
        private val deltakerlisteService = DeltakerlisteService(DeltakerlisteRepository(), mockAmtPersonServiceClient())
        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
        )
        private val arrangorMeldingProducer = mockk<ArrangorMeldingProducer>(relaxed = true)
        private val testdataService = TestdataService(
            pameldingService = pameldingService,
            deltakerService = deltakerService,
            deltakerlisteService = deltakerlisteService,
            arrangorMeldingProducer = arrangorMeldingProducer,
        )

        @JvmStatic
        @BeforeAll
        fun setup() {
            SingletonPostgres16Container
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
        clearMocks(arrangorMeldingProducer)
    }

    @Test
    fun `opprettDeltakelse - deltaker finnes ikke, gyldig request - oppretter ny deltaker`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(
            arrangor = arrangor,
            tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
        )
        val opprettetAv = TestData.lagNavAnsatt(navIdent = TESTVEILEDER)
        val opprettetAvEnhet = TestData.lagNavEnhet(enhetsnummer = TESTENHET)
        TestRepository.insert(opprettetAv)
        TestRepository.insert(opprettetAvEnhet)
        val navBruker = TestData.lagNavBruker(navVeilederId = opprettetAv.id, navEnhetId = opprettetAvEnhet.id)

        val opprettTestDeltakelseRequest = OpprettTestDeltakelseRequest(
            personident = navBruker.personident,
            deltakerlisteId = deltakerliste.id,
            startdato = LocalDate.now().minusDays(1),
            deltakelsesprosent = 60,
            dagerPerUke = 3,
        )

        val kladd = TestData.lagDeltakerKladd(
            deltakerliste = deltakerliste,
            navBruker = navBruker,
        )
        val godkjentUtkast = kladd.copy(
            dagerPerUke = opprettTestDeltakelseRequest.dagerPerUke?.toFloat(),
            deltakelsesprosent = opprettTestDeltakelseRequest.deltakelsesprosent.toFloat(),
            bakgrunnsinformasjon = null,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            kanEndres = true,
            deltakelsesinnhold = Deltakelsesinnhold(
                ledetekst = deltakerliste.tiltak.innhold!!.ledetekst,
                innhold = listOf(deltakerliste.tiltak.innhold!!.innholdselementer.first().toInnhold(valgt = true)),
            ),
        )

        TestRepository.insert(deltakerliste)
        MockResponseHandler.addOpprettKladdResponse(kladd)
        MockResponseHandler.addUtkastResponse(godkjentUtkast)

        runBlocking {
            val deltaker = testdataService.opprettDeltakelse(opprettTestDeltakelseRequest)

            val deltakerFraDb = deltakerService.getDeltakelser(navBruker.personident, deltakerliste.id).first()
            deltakerFraDb.id shouldBe deltaker.id
            deltakerFraDb.deltakerliste.id shouldBe deltakerliste.id
            deltakerFraDb.status.type shouldBe DeltakerStatus.Type.VENTER_PA_OPPSTART
            deltakerFraDb.startdato shouldBe null
            deltakerFraDb.sluttdato shouldBe null
            deltakerFraDb.dagerPerUke shouldBe opprettTestDeltakelseRequest.dagerPerUke?.toFloat()
            deltakerFraDb.deltakelsesprosent shouldBe opprettTestDeltakelseRequest.deltakelsesprosent.toFloat()
            deltakerFraDb.bakgrunnsinformasjon shouldBe null
            deltakerFraDb.deltakelsesinnhold?.ledetekst shouldBe deltakerliste.tiltak.innhold!!.ledetekst
            deltakerFraDb.deltakelsesinnhold?.innhold?.size shouldBe 1

            coEvery {
                arrangorMeldingProducer.produce(
                    match {
                        it is EndringFraArrangor && it.deltakerId == deltaker.id &&
                            it.opprettetAvArrangorAnsattId.toString() == TESTARRANGORANSATT &&
                            it.endring == EndringFraArrangor.LeggTilOppstartsdato(
                                startdato = opprettTestDeltakelseRequest.startdato,
                                sluttdato = opprettTestDeltakelseRequest.startdato.plusMonths(3),
                            )
                    },
                )
            }
        }
    }
}
