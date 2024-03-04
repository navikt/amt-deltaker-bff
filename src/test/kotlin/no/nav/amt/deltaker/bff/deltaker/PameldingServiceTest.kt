package no.nav.amt.deltaker.bff.deltaker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.VedtakRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerVedVedtak
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonClient
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFailsWith

class PameldingServiceTest {

    companion object {

        private val vedtakRepository = VedtakRepository()
        private val amtPersonClient = mockAmtPersonClient()

        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            deltakerEndringRepository = DeltakerEndringRepository(),
            navAnsattService = NavAnsattService(NavAnsattRepository(), amtPersonClient),
            navEnhetService = NavEnhetService(NavEnhetRepository(), amtPersonClient),
        )

        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            navBrukerService = NavBrukerService(NavBrukerRepository()),
            amtDeltakerClient = mockAmtDeltakerClient(),
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Before
    fun foo() {
        MockResponseHandler.addNavAnsattResponse(TestData.lagNavAnsatt())
        MockResponseHandler.addNavEnhetResponse(TestData.lagNavEnhet())
    }

    @Test
    fun `opprettKladd - deltaker finnes ikke - oppretter ny deltaker`() {
        val overordnetArrangor = TestData.lagArrangor()
        val arrangor = TestData.lagArrangor(overordnetArrangorId = overordnetArrangor.id)
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor, overordnetArrangor = overordnetArrangor)
        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltakerliste)
        TestRepository.insert(deltakerliste, overordnetArrangor)

        MockResponseHandler.addOpprettKladdResponse(kladd)

        runBlocking {
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = deltakerliste.id,
                personident = kladd.navBruker.personident,
            )

            deltaker.id shouldBe deltakerService.getDeltakelser(kladd.navBruker.personident, deltakerliste.id)
                .first().id
            deltaker.deltakerliste.id shouldBe deltakerliste.id
            deltaker.deltakerliste.navn shouldBe deltakerliste.navn
            deltaker.deltakerliste.tiltak.type shouldBe deltakerliste.tiltak.type
            deltaker.deltakerliste.arrangor.arrangor shouldBe arrangor
            deltaker.deltakerliste.arrangor.overordnetArrangorNavn shouldBe overordnetArrangor.navn
            deltaker.deltakerliste.getOppstartstype() shouldBe deltakerliste.getOppstartstype()
            deltaker.status.type shouldBe DeltakerStatus.Type.KLADD
            deltaker.startdato shouldBe null
            deltaker.sluttdato shouldBe null
            deltaker.dagerPerUke shouldBe null
            deltaker.deltakelsesprosent shouldBe null
            deltaker.bakgrunnsinformasjon shouldBe null
            deltaker.innhold shouldBe emptyList()
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
            eksisterendeDeltaker.innhold shouldBe deltaker.innhold
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
        MockResponseHandler.addOpprettKladdResponse(kladd)

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
    fun `upsertKladd - deltaker har status KLADD - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        val kladd = TestData.lagKladd(
            deltaker,
            TestData.lagPamelding(
                bakgrunnsinformasjon = "Ny og nyttig informasjon...",
                dagerPerUke = 1F,
                deltakelsesprosent = 20F,
            ),
        )

        runBlocking {
            pameldingService.upsertKladd(kladd)
            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.KLADD
            oppdatertDeltaker.bakgrunnsinformasjon shouldBe kladd.pamelding.bakgrunnsinformasjon
            oppdatertDeltaker.dagerPerUke shouldBe kladd.pamelding.dagerPerUke
            oppdatertDeltaker.deltakelsesprosent shouldBe kladd.pamelding.deltakelsesprosent
        }
    }

    @Test
    fun `upsertKladd - deltaker har ikke status KLADD - oppdaterer ikke deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        val kladd = TestData.lagKladd(
            deltaker,
            TestData.lagPamelding(
                bakgrunnsinformasjon = "Ny og nyttig informasjon...",
                dagerPerUke = 1F,
                deltakelsesprosent = 20F,
            ),
        )

        runBlocking {
            shouldThrow<IllegalArgumentException> {
                pameldingService.upsertKladd(kladd)
            }
            sammenlignDeltakere(deltakerService.get(deltaker.id).getOrThrow(), deltaker)
        }
    }

    @Test
    fun `upsertUtkast - deltaker har status KLADD - oppretter et vedtak og setter ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        val utkast = TestData.lagUtkast(deltaker)
        runBlocking {
            pameldingService.upsertUtkast(utkast)

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING

            val vedtak = vedtakRepository.getForDeltaker(deltaker.id).first()

            vedtak.deltakerId shouldBe deltaker.id
            vedtak.fattet shouldBe null
            vedtak.gyldigTil shouldBe null
            sammenlignDeltakereVedVedtak(vedtak.deltakerVedVedtak, oppdatertDeltaker)
            vedtak.fattetAvNav shouldBe false
        }
    }

    @Test
    fun `upsertUtkast - deltaker har et vedtak som ikke er fattet - oppdater eksisterende vedtak`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        val eksisterendeSamtykke = TestData.lagVedtak(
            deltakerId = deltaker.id,
            deltakerVedVedtak = deltaker,
        )
        TestRepository.insert(eksisterendeSamtykke)

        val utkast =
            TestData.lagUtkast(deltaker, TestData.lagPamelding(deltaker, bakgrunnsinformasjon = "Nye opplysninger"))

        runBlocking {
            pameldingService.upsertUtkast(utkast)

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            val vedtak = vedtakRepository.getForDeltaker(deltaker.id).first()

            vedtak.id shouldBe eksisterendeSamtykke.id
            vedtak.deltakerId shouldBe deltaker.id
            vedtak.fattet shouldBe null
            vedtak.gyldigTil shouldBe null
            sammenlignDeltakereVedVedtak(vedtak.deltakerVedVedtak, oppdatertDeltaker)
            vedtak.fattetAvNav shouldBe false
        }
    }

    @Test
    fun `upsertUtkast - deltaker har ikke status KLADD eller UTKAST - feiler`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        val opprinneligSamtykke = TestData.lagVedtak(
            deltakerId = deltaker.id,
            deltakerVedVedtak = deltaker.copy(
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            ),
            fattet = LocalDateTime.now().minusMonths(2),
            gyldigTil = null,
        )

        TestRepository.insert(deltaker)
        TestRepository.insert(opprinneligSamtykke)

        val utkast = TestData.lagUtkast(
            deltaker,
            TestData.lagPamelding(deltaker),
        )
        runBlocking {
            shouldThrow<IllegalArgumentException> {
                pameldingService.upsertUtkast(utkast)
            }
            vedtakRepository.getIkkeFattet(deltaker.id) shouldBe null
        }
    }

    @Test
    fun `avbrytUtkast - har ikke status utkast til paamelding - kaster feil`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VURDERES))
        val opprettetAv = TestData.randomNavIdent()
        val opprettetAvEnhet = TestData.randomEnhetsnummer()
        TestRepository.insert(deltaker)

        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                pameldingService.avbrytUtkast(deltaker, opprettetAvEnhet, opprettetAv)
            }
        }
    }

    @Test
    fun `avbrytUtkast - deltaker har status UTKAST_TIL_PAMELDING - avbryter utkast`() {
        val deltaker =
            TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING))
        val opprettetAv = TestData.randomNavIdent()
        val opprettetAvEnhet = TestData.randomEnhetsnummer()
        TestRepository.insert(deltaker)
        TestData.lagUtkast(deltaker)

        runBlocking {
            pameldingService.avbrytUtkast(deltaker, opprettetAvEnhet, opprettetAv)
            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.AVBRUTT_UTKAST
            oppdatertDeltaker.status.aarsak shouldBe null
        }
    }
}

fun sammenlignDeltakereVedVedtak(a: DeltakerVedVedtak, b: Deltaker) {
    sammenlignDeltakereVedVedtak(a, b.toDeltakerVedVedtak())
}

fun sammenlignDeltakereVedVedtak(a: DeltakerVedVedtak, b: DeltakerVedVedtak) {
    a.id shouldBe b.id
    a.startdato shouldBe b.startdato
    a.sluttdato shouldBe b.sluttdato
    a.dagerPerUke shouldBe b.dagerPerUke
    a.deltakelsesprosent shouldBe b.deltakelsesprosent
    a.bakgrunnsinformasjon shouldBe b.bakgrunnsinformasjon
    a.innhold shouldBe b.innhold
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
    innhold,
    status,
)
