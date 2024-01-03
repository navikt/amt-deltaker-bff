package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerHistorikkRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
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
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClientNavEnhet
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class PameldingServiceTest {

    companion object {

        private val samtykkeRepository = DeltakerSamtykkeRepository()
        private val deltakerService = DeltakerService(
            deltakerlisteRepository = DeltakerlisteRepository(),
            deltakerRepository = DeltakerRepository(),
            historikkRepository = DeltakerHistorikkRepository(),
            navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClientNavAnsatt()),
            navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClientNavEnhet()),
            deltakerProducer = DeltakerProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost())),
        )
        private val pameldingService = PameldingService(deltakerService, samtykkeRepository)

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
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

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).last()

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
