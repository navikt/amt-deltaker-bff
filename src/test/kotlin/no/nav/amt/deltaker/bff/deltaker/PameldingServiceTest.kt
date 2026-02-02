package no.nav.amt.deltaker.bff.deltaker

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.amt.deltaker.bff.DatabaseTestExtension
import no.nav.amt.deltaker.bff.deltaker.api.model.fulltInnhold
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.getInnholdselementer
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerKladd
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavEnhet
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.mockPaameldingClient
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.DeltakerVedVedtak
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.testing.shouldBeCloseTo
import no.nav.amt.lib.testing.utils.TestData.lagArrangor
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import kotlin.test.assertFailsWith

class PameldingServiceTest {
    private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val deltakerRepository = DeltakerRepository()
    private val deltakerService = DeltakerService(
        deltakerRepository = deltakerRepository,
        amtDeltakerClient = mockAmtDeltakerClient(),
        paameldingClient = mockPaameldingClient(),
        navEnhetService = navEnhetService,
        forslagRepository = mockk(),
    )

    private var pameldingService = PameldingService(
        deltakerRepository = deltakerRepository,
        deltakerService = deltakerService,
        navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
        navEnhetService = navEnhetService,
        paameldingClient = mockPaameldingClient(),
    )

    companion object {
        @JvmField
        @RegisterExtension
        val dbExtension = DatabaseTestExtension()
    }

    @Test
    fun `opprettKladd - deltaker finnes ikke - oppretter ny deltaker`() {
        val overordnetArrangorInTest = lagArrangor()
        val arrangorInTest = lagArrangor(overordnetArrangorId = overordnetArrangorInTest.id)
        val deltakerListeInTest = lagDeltakerliste(arrangor = arrangorInTest, overordnetArrangor = overordnetArrangorInTest)
        val kladdInTest = lagDeltakerKladd(deltakerliste = deltakerListeInTest)
        val navVeilederInTest = lagNavAnsatt(id = kladdInTest.navBruker.navVeilederId!!)
        val navEnhetInTest = lagNavEnhet(id = kladdInTest.navBruker.navEnhetId!!)
        TestRepository.insert(deltakerListeInTest, overordnetArrangorInTest)

        MockResponseHandler.addOpprettKladdResponse(kladdInTest)
        MockResponseHandler.addNavAnsattResponse(navVeilederInTest)
        MockResponseHandler.addNavEnhetGetResponse(navEnhetInTest)

        runBlocking {
            val deltaker = pameldingService.opprettDeltaker(
                deltakerlisteId = deltakerListeInTest.id,
                personIdent = kladdInTest.navBruker.personident,
            )

            deltaker.id shouldBe deltakerRepository
                .getMany(kladdInTest.navBruker.personident, deltakerListeInTest.id)
                .first()
                .id

            assertSoftly(deltaker) {
                status.type shouldBe DeltakerStatus.Type.KLADD
                startdato shouldBe null
                sluttdato shouldBe null
                dagerPerUke shouldBe null
                deltakelsesprosent shouldBe null
                bakgrunnsinformasjon shouldBe null
                deltakelsesinnhold!!.innhold shouldBe emptyList()

                assertSoftly(deltakerListeInTest) {
                    id shouldBe deltakerListeInTest.id
                    navn shouldBe deltakerListeInTest.navn
                    tiltak.tiltakskode shouldBe deltakerListeInTest.tiltak.tiltakskode
                    it.arrangor.arrangor shouldBe arrangorInTest
                    it.arrangor.overordnetArrangorNavn shouldBe overordnetArrangorInTest.navn
                    it.oppstart shouldBe deltakerListeInTest.oppstart
                }
            }
        }
    }

    @Test
    fun `opprettKladd - kall til amt-deltaker feiler - kaster exception`() {
        val personIdent = TestData.randomIdent()
        MockResponseHandler.addOpprettKladdResponse(null)
        runBlocking {
            assertFailsWith<IllegalStateException> {
                pameldingService.opprettDeltaker(UUID.randomUUID(), personIdent)
            }
        }
    }

    @Test
    fun `upsertUtkast - oppdaterer og returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING))
        val navEnhet = lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
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

    @Nested
    inner class AvbrytUtkast {
        @Test
        fun `avbrytUtkast() - utkast avbrytes for ny deltakelse - Den forrige avsluttede deltakelsen laases opp`() = runTest {
            val navEnhet = lagNavEnhet()
            TestRepository.insert(navEnhet)

            val gammelDeltaker = TestData.lagDeltaker(
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
                navBruker = TestData.lagNavBruker(navEnhetId = navEnhet.id),
            )
            TestRepository.insert(gammelDeltaker)

            val nyDeltaker = lagDeltakerKladd(
                deltakerliste = gammelDeltaker.deltakerliste,
                navBruker = gammelDeltaker.navBruker,
            )
            TestRepository.insert(nyDeltaker)

            val nyDeltakerOppdaterUtkast = Deltakeroppdatering(
                id = nyDeltaker.id,
                startdato = null,
                sluttdato = null,
                dagerPerUke = null,
                deltakelsesprosent = 100F,
                bakgrunnsinformasjon = "Tekst",
                deltakelsesinnhold = null,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
                erManueltDeltMedArrangor = false,
                historikk = emptyList(),
            )
            deltakerService.oppdaterDeltaker(nyDeltakerOppdaterUtkast)

            deltakerRepository.get(gammelDeltaker.id).getOrThrow().kanEndres shouldBe false

            MockResponseHandler.avbrytUtkastResponse(nyDeltaker)
            pameldingService.avbrytUtkast(nyDeltaker, navEnhet.enhetsnummer, "test")

            val gammelDeltakerFraDb = deltakerRepository.get(gammelDeltaker.id).getOrThrow()
            gammelDeltakerFraDb.kanEndres shouldBe true
        }

        @Test
        fun `avbrytUtkast() - utkast avbrytes for ny deltakelse - Den forrige avsluttede deltakelsen forblir laast`() = runTest {
            val navEnhet = lagNavEnhet()
            TestRepository.insert(navEnhet)

            val gammelDeltaker = TestData.lagDeltaker(
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FEILREGISTRERT),
                navBruker = TestData.lagNavBruker(navEnhetId = navEnhet.id),
            )
            TestRepository.insert(gammelDeltaker)

            val nyDeltaker = lagDeltakerKladd(
                deltakerliste = gammelDeltaker.deltakerliste,
                navBruker = gammelDeltaker.navBruker,
            )
            TestRepository.insert(nyDeltaker)

            val nyDeltakerOppdaterUtkast = Deltakeroppdatering(
                id = nyDeltaker.id,
                startdato = null,
                sluttdato = null,
                dagerPerUke = null,
                deltakelsesprosent = 100F,
                bakgrunnsinformasjon = "Tekst",
                deltakelsesinnhold = null,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
                erManueltDeltMedArrangor = false,
                historikk = emptyList(),
            )

            deltakerService.oppdaterDeltaker(nyDeltakerOppdaterUtkast)

            deltakerRepository.get(gammelDeltaker.id).getOrThrow().kanEndres shouldBe false

            MockResponseHandler.avbrytUtkastResponse(nyDeltaker)

            pameldingService.avbrytUtkast(nyDeltaker, navEnhet.enhetsnummer, "test")

            val gammelDeltakerFraDb = deltakerRepository.get(gammelDeltaker.id).getOrThrow()
            gammelDeltakerFraDb.kanEndres shouldBe false
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
                getInnholdselementer(deltakerliste.tiltak.innhold?.innholdselementer, deltakerliste.tiltak.tiltakskode),
            ),
        )
    },
    status,
)
