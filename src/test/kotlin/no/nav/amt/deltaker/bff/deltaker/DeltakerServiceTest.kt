package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerHistorikkRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFailsWith

class DeltakerServiceTest {
    private val personident = "12345678910"
    private val opprettetAv = "OpprettetAv"
    private val opprettetAvEnhet = TestData.randomEnhetsnummer()

    companion object {
        lateinit var deltakerlisteRepository: DeltakerlisteRepository
        lateinit var deltakerRepository: DeltakerRepository
        lateinit var deltakerService: DeltakerService
        lateinit var samtykkeRepository: DeltakerSamtykkeRepository
        lateinit var historikkRepository: DeltakerHistorikkRepository
        lateinit var navAnsattRepository: NavAnsattRepository
        lateinit var navAnsattService: NavAnsattService
        lateinit var navEnhetRepository: NavEnhetRepository
        lateinit var navEnhetService: NavEnhetService

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            deltakerlisteRepository = DeltakerlisteRepository()
            deltakerRepository = DeltakerRepository()
            samtykkeRepository = DeltakerSamtykkeRepository()
            historikkRepository = DeltakerHistorikkRepository()
            navAnsattRepository = NavAnsattRepository()
            navEnhetRepository = NavEnhetRepository()
            navAnsattService = NavAnsattService(navAnsattRepository, mockAmtPersonServiceClientNavAnsatt())
            navEnhetService = NavEnhetService(navEnhetRepository, mockAmtPersonServiceClientNavEnhet())
            deltakerService = DeltakerService(
                deltakerRepository,
                deltakerlisteRepository,
                samtykkeRepository,
                historikkRepository,
                navAnsattService,
                navEnhetService,
            )
        }
    }

    @Test
    fun `opprettDeltaker - deltaker finnes ikke - oppretter ny deltaker`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        deltakerlisteRepository.upsert(deltakerliste)

        runBlocking {
            val deltakerResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv, opprettetAvEnhet)

            deltakerResponse.deltakerId shouldBe deltakerRepository.get(personident, deltakerliste.id)?.id
            deltakerResponse.deltakerliste.deltakerlisteId shouldBe deltakerliste.id
            deltakerResponse.deltakerliste.deltakerlisteNavn shouldBe deltakerliste.navn
            deltakerResponse.deltakerliste.tiltakstype shouldBe deltakerliste.tiltak.type
            deltakerResponse.deltakerliste.arrangorNavn shouldBe arrangor.navn
            deltakerResponse.deltakerliste.oppstartstype shouldBe deltakerliste.getOppstartstype()
            deltakerResponse.status.type shouldBe DeltakerStatus.Type.KLADD
            deltakerResponse.startdato shouldBe null
            deltakerResponse.sluttdato shouldBe null
            deltakerResponse.dagerPerUke shouldBe null
            deltakerResponse.deltakelsesprosent shouldBe null
            deltakerResponse.bakgrunnsinformasjon shouldBe null
            deltakerResponse.mal shouldBe emptyList()
        }
    }

    @Test
    fun `opprettDeltaker - deltakerliste finnes ikke - kaster NoSuchElementException`() {
        runBlocking {
            assertFailsWith<NoSuchElementException> {
                deltakerService.opprettDeltaker(UUID.randomUUID(), personident, opprettetAv, opprettetAvEnhet)
            }
        }
    }

    @Test
    fun `opprettDeltaker - deltaker finnes og deltar fortsatt - returnerer eksisterende deltaker`() {
        val deltakerId = UUID.randomUUID()
        val deltaker = TestData.lagDeltaker(
            id = deltakerId,
            personident = personident,
            sluttdato = null,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
        )
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(id = deltaker.deltakerlisteId, arrangor = arrangor)
        TestRepository.insert(arrangor)
        deltakerlisteRepository.upsert(deltakerliste)
        deltakerRepository.upsert(deltaker)

        runBlocking {
            val deltakerResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv, opprettetAvEnhet)

            deltakerResponse.deltakerId shouldBe deltakerId
            deltakerResponse.status.type shouldBe DeltakerStatus.Type.DELTAR
            deltakerResponse.startdato shouldBe deltaker.startdato
            deltakerResponse.sluttdato shouldBe deltaker.sluttdato
            deltakerResponse.dagerPerUke shouldBe deltaker.dagerPerUke
            deltakerResponse.deltakelsesprosent shouldBe deltaker.deltakelsesprosent
            deltakerResponse.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
            deltakerResponse.mal shouldBe deltaker.mal
        }
    }

    @Test
    fun `opprettDeltaker - deltaker finnes men har sluttet - oppretter ny deltaker`() {
        val deltakerId = UUID.randomUUID()
        val deltaker = TestData.lagDeltaker(
            id = deltakerId,
            personident = personident,
            sluttdato = LocalDate.now().minusWeeks(3),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(id = deltaker.deltakerlisteId, arrangor = arrangor)
        TestRepository.insert(arrangor)
        deltakerlisteRepository.upsert(deltakerliste)
        deltakerRepository.upsert(deltaker)

        runBlocking {
            val deltakerResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv, opprettetAvEnhet)

            deltakerResponse.deltakerId shouldNotBe deltakerId
            deltakerResponse.status.type shouldBe DeltakerStatus.Type.KLADD
        }
    }

    @Test
    fun `opprettUtkast - deltaker har status KLADD - oppretter et samtykke og setter ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        val utkast = TestData.lagOppdatertDeltaker()
        runBlocking {
            deltakerService.opprettUtkast(deltaker, utkast, TestData.randomNavIdent(), TestData.randomEnhetsnummer())

            val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe null
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
            deltakerService.opprettUtkast(deltaker, utkast, TestData.randomNavIdent(), TestData.randomEnhetsnummer())

            val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.id shouldBe eksisterendeSamtykke.id
            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe null
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
        )
        runBlocking {
            deltakerService.opprettUtkast(deltaker, utkast, navIdent, navEnhet)

            val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
            oppdatertDeltaker.status.type shouldBe deltaker.status.type
            oppdatertDeltaker.sistEndretAv shouldBe navIdent

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).last()

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBe null
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
            samtykke.godkjentAvNav shouldBe utkast.godkjentAvNav
        }
    }

    @Test
    fun `meldPaUtenGodkjenning - deltaker har status KLADD - oppretter et godkjent samtykke og setter ny status for deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)
        val godkjenningAvNav = TestData.lagGodkjenningAvNav()
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(godkjentAvNav = godkjenningAvNav)

        runBlocking {
            deltakerService.meldPaUtenGodkjenning(deltaker, oppdatertDeltaker, godkjenningAvNav.godkjentAv, godkjenningAvNav.godkjentAvEnhet)

            val oppdatertDeltakerFraDb = deltakerRepository.get(deltaker.id)!!
            oppdatertDeltakerFraDb.status.type shouldBe DeltakerStatus.Type.VENTER_PA_OPPSTART

            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltakerFraDb)
            samtykke.godkjentAvNav shouldBe godkjenningAvNav
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
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(bakgrunnsinformasjon = "Nye opplysninger...", godkjentAvNav = godkjenningAvNav)

        runBlocking {
            deltakerService.meldPaUtenGodkjenning(deltaker, oppdatertDeltaker, godkjenningAvNav.godkjentAv, godkjenningAvNav.godkjentAvEnhet)

            val oppdatertDeltakerFraDb = deltakerRepository.get(deltaker.id)!!
            val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

            samtykke.id shouldBe eksisterendeSamtykke.id
            samtykke.deltakerId shouldBe deltaker.id
            samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
            samtykke.gyldigTil shouldBe null
            sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltakerFraDb)
            samtykke.godkjentAvNav shouldBe godkjenningAvNav
        }
    }

    @Test
    fun `meldPaUtenGodkjenning - oppdatert deltaker mangler godkjenning - kaster feil`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = TestData.lagOppdatertDeltaker(godkjentAvNav = null)

        runBlocking {
            assertFailsWith<RuntimeException> {
                deltakerService.meldPaUtenGodkjenning(deltaker, oppdatertDeltaker, TestData.randomNavIdent(), TestData.randomEnhetsnummer())
            }
        }
    }

    @Test
    fun `oppdaterDeltaker - oppdatert bakgrunnsinformasjon - oppdaterer i databasen og returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        TestRepository.insert(deltaker)
        val oppdatertBakgrunnsinformasjon = "Oppdatert informasjon"
        val endretAv = TestData.randomNavIdent()
        val endretAvEnhet = TestData.randomEnhetsnummer()

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndringType.BAKGRUNNSINFORMASJON,
                DeltakerEndring.EndreBakgrunnsinformasjon(oppdatertBakgrunnsinformasjon),
                endretAv,
                endretAvEnhet,
            )

            oppdatertDeltaker.bakgrunnsinformasjon shouldBe oppdatertBakgrunnsinformasjon
            val oppdatertDeltakerFraDb = deltakerService.get(deltaker.id)
            oppdatertDeltakerFraDb.sistEndretAv shouldBe endretAv
            oppdatertDeltakerFraDb.sistEndret shouldBeCloseTo LocalDateTime.now()
            val historikk = historikkRepository.getForDeltaker(deltaker.id)
            historikk.size shouldBe 1
            historikk.first().endringType shouldBe DeltakerEndringType.BAKGRUNNSINFORMASJON
            historikk.first().endring shouldBe DeltakerEndring.EndreBakgrunnsinformasjon(oppdatertBakgrunnsinformasjon)
            historikk.first().endret shouldBeCloseTo LocalDateTime.now()
            historikk.first().endretAv shouldBe endretAv
            historikk.first().endretAvEnhet shouldBe endretAvEnhet
        }
    }

    @Test
    fun `oppdaterDeltaker - oppdatert bakgrunnsinformasjon, men ingen endring - oppdaterer ikke i databasen og returnerer uendret deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        TestRepository.insert(deltaker)
        val endretAv = TestData.randomNavIdent()
        val endretAvEnhet = TestData.randomEnhetsnummer()

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndringType.BAKGRUNNSINFORMASJON,
                DeltakerEndring.EndreBakgrunnsinformasjon(deltaker.bakgrunnsinformasjon),
                endretAv,
                endretAvEnhet,
            )

            oppdatertDeltaker.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
            val oppdatertDeltakerFraDb = deltakerService.get(deltaker.id)
            oppdatertDeltakerFraDb.sistEndretAv shouldBe deltaker.sistEndretAv
            oppdatertDeltakerFraDb.sistEndret shouldBeCloseTo deltaker.sistEndret
            val historikk = historikkRepository.getForDeltaker(deltaker.id)
            historikk.size shouldBe 0
        }
    }

    @Test
    fun `oppdaterDeltaker - oppdatert bakgrunnsinformasjon, deltaker har sluttet - kaster feil`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET), sluttdato = LocalDate.now().minusMonths(1))
        TestRepository.insert(deltaker)

        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                deltakerService.oppdaterDeltaker(
                    deltaker,
                    DeltakerEndringType.BAKGRUNNSINFORMASJON,
                    DeltakerEndring.EndreBakgrunnsinformasjon("oppdatert informasjon"),
                    TestData.randomNavIdent(),
                    TestData.randomEnhetsnummer(),
                )
            }
        }
    }
}
