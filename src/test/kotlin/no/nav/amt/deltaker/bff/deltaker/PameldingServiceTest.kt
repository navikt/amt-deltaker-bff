package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.api.model.fulltInnhold
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.getInnholdselementerMedAnnet
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.DeltakerVedVedtak
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

class PameldingServiceTest {
    companion object {
        private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
        private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            amtDeltakerClient = mockAmtDeltakerClient(),
            navEnhetService = navEnhetService,
            forslagService = mockk(),
        )

        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
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
    fun `opprettKladd - deltaker finnes ikke - oppretter ny deltaker`() {
        val overordnetArrangor = TestData.lagArrangor()
        val arrangor = TestData.lagArrangor(overordnetArrangorId = overordnetArrangor.id)
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor, overordnetArrangor = overordnetArrangor)
        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltakerliste)
        val navVeileder = TestData.lagNavAnsatt(id = kladd.navBruker.navVeilederId!!)
        val navEnhet = TestData.lagNavEnhet(id = kladd.navBruker.navEnhetId!!)
        TestRepository.insert(deltakerliste, overordnetArrangor)

        MockResponseHandler.addOpprettKladdResponse(kladd)
        MockResponseHandler.addNavAnsattResponse(navVeileder)
        MockResponseHandler.addNavEnhetGetResponse(navEnhet)

        runBlocking {
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = deltakerliste.id,
                personident = kladd.navBruker.personident,
            )

            deltaker.id shouldBe deltakerService
                .getDeltakelser(kladd.navBruker.personident, deltakerliste.id)
                .first()
                .id
            deltaker.deltakerliste.id shouldBe deltakerliste.id
            deltaker.deltakerliste.navn shouldBe deltakerliste.navn
            deltaker.deltakerliste.tiltak.arenaKode shouldBe deltakerliste.tiltak.arenaKode
            deltaker.deltakerliste.arrangor.arrangor shouldBe arrangor
            deltaker.deltakerliste.arrangor.overordnetArrangorNavn shouldBe overordnetArrangor.navn
            deltaker.deltakerliste.getOppstartstype() shouldBe deltakerliste.getOppstartstype()
            deltaker.status.type shouldBe DeltakerStatus.Type.KLADD
            deltaker.startdato shouldBe null
            deltaker.sluttdato shouldBe null
            deltaker.dagerPerUke shouldBe null
            deltaker.deltakelsesprosent shouldBe null
            deltaker.bakgrunnsinformasjon shouldBe null
            deltaker.deltakelsesinnhold!!.innhold shouldBe emptyList()
        }
    }

    @Test
    fun `opprettKladd - kall til amt-deltaker feiler - kaster exception`() {
        val personident = TestData.randomIdent()
        MockResponseHandler.addOpprettKladdResponse(null)
        runBlocking {
            assertFailsWith<IllegalStateException> {
                pameldingService.opprettKladd(UUID.randomUUID(), personident)
            }
        }
    }

    @Test
    fun `opprettKladd - deltaker finnes og deltar fortsatt - returnerer eksisterende deltaker`() {
        val deltaker = TestData.lagDeltaker(
            sluttdato = null,
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltaker)

        runBlocking {
            val eksisterendeDeltaker =
                pameldingService.opprettKladd(
                    deltaker.deltakerliste.id,
                    deltaker.navBruker.personident,
                )

            eksisterendeDeltaker.id shouldBe deltaker.id
            eksisterendeDeltaker.status.type shouldBe DeltakerStatus.Type.DELTAR
            eksisterendeDeltaker.startdato shouldBe deltaker.startdato
            eksisterendeDeltaker.sluttdato shouldBe deltaker.sluttdato
            eksisterendeDeltaker.dagerPerUke shouldBe deltaker.dagerPerUke
            eksisterendeDeltaker.deltakelsesprosent shouldBe deltaker.deltakelsesprosent
            eksisterendeDeltaker.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
            eksisterendeDeltaker.deltakelsesinnhold shouldBe deltaker.deltakelsesinnhold
        }
    }

    @Test
    fun `opprettKladd - deltaker finnes men har sluttet - oppretter ny deltaker`() {
        val deltaker = TestData.lagDeltaker(
            sluttdato = LocalDate.now().minusMonths(3),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(deltaker)

        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltaker.deltakerliste)
        val navVeileder = TestData.lagNavAnsatt(kladd.navBruker.navVeilederId!!)
        val navEnhet = TestData.lagNavEnhet(kladd.navBruker.navEnhetId!!)
        MockResponseHandler.addOpprettKladdResponse(kladd)
        MockResponseHandler.addNavAnsattResponse(navVeileder)
        MockResponseHandler.addNavEnhetGetResponse(navEnhet)

        runBlocking {
            val nyDeltaker =
                pameldingService.opprettKladd(
                    deltaker.deltakerliste.id,
                    deltaker.navBruker.personident,
                )

            nyDeltaker.id shouldNotBe deltaker.id
            nyDeltaker.status.type shouldBe DeltakerStatus.Type.KLADD
        }
    }

    @Test
    fun `upsertUtkast - oppdaterer og returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING))
        val navEnhet = TestData.lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
        TestRepository.insert(navEnhet)
        TestRepository.insert(deltaker)

        val forventetDeltaker = deltaker.copy(
            deltakelsesinnhold = Deltakelsesinnhold(
                "Beskrivelse",
                listOf(Innhold("nytt innhold", "nytt-innhold", true, null)),
            ),
            bakgrunnsinformasjon = "Noe ny informasjon",
            deltakelsesprosent = 42F,
            dagerPerUke = 3F,
        )

        MockResponseHandler.addUtkastResponse(forventetDeltaker)

        val utkast = Utkast(
            deltaker.id,
            Pamelding(
                forventetDeltaker.deltakelsesinnhold!!,
                forventetDeltaker.bakgrunnsinformasjon,
                forventetDeltaker.deltakelsesprosent,
                forventetDeltaker.dagerPerUke,
                endretAv = "Veileder",
                endretAvEnhet = navEnhet.enhetsnummer,
            ),
            false,
        )

        runBlocking {
            val oppdatertDeltaker = pameldingService.upsertUtkast(utkast)
            oppdatertDeltaker.deltakelsesinnhold shouldBe forventetDeltaker.deltakelsesinnhold
            oppdatertDeltaker.bakgrunnsinformasjon shouldBe forventetDeltaker.bakgrunnsinformasjon
            oppdatertDeltaker.deltakelsesprosent shouldBe forventetDeltaker.deltakelsesprosent
            oppdatertDeltaker.dagerPerUke shouldBe forventetDeltaker.dagerPerUke
        }
    }
}

fun sammenlignDeltakereVedVedtak(a: DeltakerVedVedtak, b: DeltakerVedVedtak) {
    a.id shouldBe b.id
    a.startdato shouldBe b.startdato
    a.sluttdato shouldBe b.sluttdato
    a.dagerPerUke shouldBe b.dagerPerUke
    a.deltakelsesprosent shouldBe b.deltakelsesprosent
    a.bakgrunnsinformasjon shouldBe b.bakgrunnsinformasjon
    a.deltakelsesinnhold shouldBe b.deltakelsesinnhold
    a.status.id shouldBe b.status.id
    a.status.type shouldBe b.status.type
    a.status.aarsak shouldBe b.status.aarsak
    a.status.gyldigFra shouldBeCloseTo b.status.gyldigFra
    a.status.gyldigTil shouldBeCloseTo b.status.gyldigTil
    a.status.opprettet shouldBeCloseTo b.status.opprettet
}

fun Deltaker.toDeltakerVedVedtak() = DeltakerVedVedtak(
    id,
    startdato,
    sluttdato,
    dagerPerUke,
    deltakelsesprosent,
    bakgrunnsinformasjon,
    deltakelsesinnhold = deltakelsesinnhold?.let {
        Deltakelsesinnhold(
            ledetekst = it.ledetekst,
            innhold = fulltInnhold(
                it.innhold,
                deltakerliste.tiltak.innhold?.getInnholdselementerMedAnnet(deltakerliste.tiltak.tiltakskode) ?: emptyList(),
            ),
        )
    },
    status,
)
