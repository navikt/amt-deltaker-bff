package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import no.nav.amt.deltaker.bff.kafka.utils.SingletonKafkaProvider
import no.nav.amt.deltaker.bff.kafka.utils.assertProduced
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClientNavAnsatt
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClientNavBruker
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClientNavEnhet
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFailsWith

class PameldingServiceTest {

    companion object {

        private val samtykkeRepository = DeltakerSamtykkeRepository()
        private val deltakerService = DeltakerService(
            deltakerRepository = DeltakerRepository(),
            deltakerEndringRepository = DeltakerEndringRepository(),
            navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClientNavAnsatt()),
            navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClientNavEnhet()),
            deltakerProducer = DeltakerProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost())),
        )

        private var pameldingService = PameldingService(
            deltakerService = deltakerService,
            samtykkeRepository = samtykkeRepository,
            deltakerlisteRepository = DeltakerlisteRepository(),
            navBrukerService = NavBrukerService(NavBrukerRepository(), mockAmtPersonServiceClientNavBruker()),
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    fun mockPameldingService(navBruker: NavBruker) {
        pameldingService = PameldingService(
            deltakerService,
            samtykkeRepository,
            deltakerlisteRepository = DeltakerlisteRepository(),
            navBrukerService = NavBrukerService(NavBrukerRepository(), mockAmtPersonServiceClientNavBruker(navBruker)),
        )
    }

    @Test
    fun `opprettKladd - deltaker finnes ikke - oppretter ny deltaker`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        val navBruker = TestData.lagNavBruker()
        val opprettetAv = TestData.randomNavIdent()
        val opprettetAvEnhet = TestData.randomEnhetsnummer()
        TestRepository.insert(deltakerliste)
        mockPameldingService(navBruker = navBruker)

        runBlocking {
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = deltakerliste.id,
                personident = navBruker.personident,
                opprettetAv = opprettetAv,
                opprettetAvEnhet = opprettetAvEnhet,
            )

            deltaker.id shouldBe deltakerService.get(navBruker.personident, deltakerliste.id).getOrThrow().id
            deltaker.deltakerliste.id shouldBe deltakerliste.id
            deltaker.deltakerliste.navn shouldBe deltakerliste.navn
            deltaker.deltakerliste.tiltak.type shouldBe deltakerliste.tiltak.type
            deltaker.deltakerliste.arrangor.navn shouldBe arrangor.navn
            deltaker.deltakerliste.getOppstartstype() shouldBe deltakerliste.getOppstartstype()
            deltaker.status.type shouldBe DeltakerStatus.Type.KLADD
            deltaker.startdato shouldBe null
            deltaker.sluttdato shouldBe null
            deltaker.dagerPerUke shouldBe null
            deltaker.deltakelsesprosent shouldBe null
            deltaker.bakgrunnsinformasjon shouldBe null
            deltaker.mal shouldBe emptyList()
        }
    }

    @Test
    fun `opprettKladd - deltakerliste finnes ikke - kaster NoSuchElementException`() {
        val personident = TestData.randomIdent()
        val opprettetAv = TestData.randomNavIdent()
        val opprettetAvEnhet = TestData.randomEnhetsnummer()
        runBlocking {
            assertFailsWith<NoSuchElementException> {
                pameldingService.opprettKladd(UUID.randomUUID(), personident, opprettetAv, opprettetAvEnhet)
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
                    deltaker.sistEndretAv,
                    deltaker.sistEndretAvEnhet,
                )

            eksisterendeDeltaker.id shouldBe deltaker.id
            eksisterendeDeltaker.status.type shouldBe DeltakerStatus.Type.DELTAR
            eksisterendeDeltaker.startdato shouldBe deltaker.startdato
            eksisterendeDeltaker.sluttdato shouldBe deltaker.sluttdato
            eksisterendeDeltaker.dagerPerUke shouldBe deltaker.dagerPerUke
            eksisterendeDeltaker.deltakelsesprosent shouldBe deltaker.deltakelsesprosent
            eksisterendeDeltaker.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
            eksisterendeDeltaker.mal shouldBe deltaker.mal
        }
    }

    @Test
    fun `opprettKladd - deltaker finnes men har sluttet - oppretter ny deltaker`() {
        val deltaker = TestData.lagDeltaker(
            sluttdato = LocalDate.now().minusWeeks(3),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(deltaker)

        runBlocking {
            val nyDeltaker =
                pameldingService.opprettKladd(
                    deltaker.deltakerliste.id,
                    deltaker.navBruker.personident,
                    deltaker.sistEndretAv,
                    deltaker.sistEndretAvEnhet,
                )

            nyDeltaker.id shouldNotBe deltaker.id
            nyDeltaker.status.type shouldBe DeltakerStatus.Type.KLADD
        }
    }

    @Test
    fun `opprettUtkast - deltaker har status KLADD - oppretter et samtykke og setter ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        val utkast = TestData.lagOppdatertDeltaker()
        runBlocking {
            pameldingService.opprettUtkast(deltaker, utkast)

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe null

            assertProduced(oppdatertDeltaker)
        }
    }

    @Test
    fun `opprettUtkast - deltaker har et samtykke som ikke er godkjent - oppdater eksisterende samtykke`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        val eksisterendeSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(eksisterendeSamtykke)

        val utkast = TestData.lagOppdatertDeltaker(bakgrunnsinformasjon = "Nye opplysninger...")

        runBlocking {
            pameldingService.opprettUtkast(deltaker, utkast)

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.id shouldBe eksisterendeSamtykke.id
            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe null

            assertProduced(oppdatertDeltaker)
        }
    }

    @Test
    fun `opprettUtkast - deltaker har ikke status KLADD - oppretter et samtykke og setter ikke ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        val opprinneligSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker.copy(
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            ),
            godkjent = LocalDateTime.now().minusMonths(2),
            gyldigTil = null,
        )

        TestRepository.insert(deltaker)
        TestRepository.insert(opprinneligSamtykke)

        val navIdent = TestData.randomNavIdent()
        val navEnhet = TestData.randomEnhetsnummer()
        val utkast = TestData.lagOppdatertDeltaker(
            godkjentAvNav = TestData.lagGodkjenningAvNav(godkjentAv = navIdent, godkjentAvEnhet = navEnhet),
            endretAv = navIdent,
            endretAvEnhet = navEnhet,
        )
        runBlocking {
            pameldingService.opprettUtkast(deltaker, utkast)

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.status.type shouldBe deltaker.status.type
            oppdatertDeltaker.sistEndretAv shouldBe navIdent

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).find { it.godkjent == null }!!

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe utkast.godkjentAvNav

            assertProduced(oppdatertDeltaker)
        }
    }

    @Test
    fun `meldPaUtenGodkjenning - deltaker har status KLADD - oppretter et godkjent samtykke og setter ny status for deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)
        val godkjenningAvNav = TestData.lagGodkjenningAvNav()
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(godkjentAvNav = godkjenningAvNav)

        runBlocking {
            pameldingService.meldPaUtenGodkjenning(
                deltaker,
                oppdatertDeltaker,
            )

            val oppdatertDeltakerFraDb = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltakerFraDb.status.type shouldBe DeltakerStatus.Type.VENTER_PA_OPPSTART

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltakerFraDb)
            samtykke.godkjentAvNav shouldBe godkjenningAvNav

            assertProduced(oppdatertDeltakerFraDb)
        }
    }

    @Test
    fun `meldPaUtenGodkjenning - deltaker har et samtykke som ikke er godkjent - oppdater eksisterende samtykke`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)
        val eksisterendeSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(eksisterendeSamtykke)
        val godkjenningAvNav = TestData.lagGodkjenningAvNav()
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(
            bakgrunnsinformasjon = "Nye opplysninger...",
            godkjentAvNav = godkjenningAvNav,
        )

        runBlocking {
            pameldingService.meldPaUtenGodkjenning(deltaker, oppdatertDeltaker)

            val oppdatertDeltakerFraDb = deltakerService.get(deltaker.id).getOrThrow()
            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.id shouldBe eksisterendeSamtykke.id
            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltakerFraDb)
            samtykke.godkjentAvNav shouldBe godkjenningAvNav

            assertProduced(oppdatertDeltakerFraDb)
        }
    }

    @Test
    fun `meldPaUtenGodkjenning - oppdatert deltaker mangler godkjenning - kaster feil`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(godkjentAvNav = null)

        runBlocking {
            assertFailsWith<RuntimeException> {
                pameldingService.meldPaUtenGodkjenning(
                    deltaker,
                    oppdatertDeltaker,
                )
            }
        }
    }
}
