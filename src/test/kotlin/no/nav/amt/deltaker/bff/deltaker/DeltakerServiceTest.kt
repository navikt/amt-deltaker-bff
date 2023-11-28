package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
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

    companion object {
        lateinit var deltakerlisteRepository: DeltakerlisteRepository
        lateinit var deltakerRepository: DeltakerRepository
        lateinit var deltakerService: DeltakerService
        lateinit var samtykkeRepository: DeltakerSamtykkeRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            deltakerlisteRepository = DeltakerlisteRepository()
            deltakerRepository = DeltakerRepository()
            samtykkeRepository = DeltakerSamtykkeRepository()
            deltakerService = DeltakerService(deltakerRepository, deltakerlisteRepository, samtykkeRepository)
        }
    }

    @Test
    fun `opprettDeltaker - deltaker finnes ikke - oppretter ny deltaker`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        deltakerlisteRepository.upsert(deltakerliste)

        val pameldingResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv)

        pameldingResponse.deltakerId shouldBe deltakerRepository.get(personident, deltakerliste.id)?.id
        pameldingResponse.deltakerliste.deltakerlisteId shouldBe deltakerliste.id
        pameldingResponse.deltakerliste.deltakerlisteNavn shouldBe deltakerliste.navn
        pameldingResponse.deltakerliste.tiltakstype shouldBe deltakerliste.tiltak.type
        pameldingResponse.deltakerliste.arrangorNavn shouldBe arrangor.navn
        pameldingResponse.deltakerliste.oppstartstype shouldBe deltakerliste.getOppstartstype()
        pameldingResponse.deltakerliste.mal shouldBe emptyList()
    }

    @Test
    fun `opprettDeltaker - deltakerliste finnes ikke - kaster NoSuchElementException`() {
        assertFailsWith<NoSuchElementException> {
            deltakerService.opprettDeltaker(UUID.randomUUID(), personident, opprettetAv)
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

        val pameldingResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv)

        pameldingResponse.deltakerId shouldBe deltakerId
    }

    @Test
    fun `opprettDeltaker - deltaker finnes men har sluttet - oppretter ny deltaker`() {
        val deltakerId = UUID.randomUUID()
        val deltaker = TestData.lagDeltaker(
            id = deltakerId,
            personident = personident,
            sluttdato = LocalDate.now().minusDays(2),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(id = deltaker.deltakerlisteId, arrangor = arrangor)
        TestRepository.insert(arrangor)
        deltakerlisteRepository.upsert(deltakerliste)
        deltakerRepository.upsert(deltaker)

        val pameldingResponse = deltakerService.opprettDeltaker(deltakerliste.id, personident, opprettetAv)

        pameldingResponse.deltakerId shouldNotBe deltakerId
    }

    @Test
    fun `opprettForslag - deltaker har status UTKAST - oppretter et samtykke og setter ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST))
        TestRepository.insert(deltaker)

        val forslag = TestData.lagForslagTilDeltaker()
        deltakerService.opprettForslag(deltaker, forslag, TestData.randomNavIdent())

        val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
        oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER

        val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

        samtykke.deltakerId shouldBe deltaker.id
        samtykke.godkjent shouldBe null
        samtykke.gyldigTil shouldBe null
        sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
        samtykke.godkjentAvNav shouldBe null
    }

    @Test
    fun `opprettForslag - deltaker har et samtykke som ikke er godkjent - oppdater eksisterende samtykke`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER),
        )
        TestRepository.insert(deltaker)

        val eksisterendeSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(eksisterendeSamtykke)

        val forslag = TestData.lagForslagTilDeltaker(bakgrunnsinformasjon = "Nye opplysninger...")

        deltakerService.opprettForslag(deltaker, forslag, TestData.randomNavIdent())

        val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
        val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

        samtykke.id shouldBe eksisterendeSamtykke.id
        samtykke.deltakerId shouldBe deltaker.id
        samtykke.godkjent shouldBe null
        samtykke.gyldigTil shouldBe null
        sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
        samtykke.godkjentAvNav shouldBe null
    }

    @Test
    fun `opprettForslag - deltaker har ikke status UTKAST - oppretter et samtykke og setter ikke ny status på deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        val opprinneligSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker.copy(
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER),
            ),
            godkjent = LocalDateTime.now().minusMonths(2),
            gyldigTil = null,
        )

        TestRepository.insert(deltaker)
        TestRepository.insert(opprinneligSamtykke)

        val navIdent = TestData.randomNavIdent()
        val forslag = TestData.lagForslagTilDeltaker(
            godkjentAvNav = TestData.lagGodkjenningAvNav(godkjentAv = navIdent),
        )
        deltakerService.opprettForslag(deltaker, forslag, navIdent)

        val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
        oppdatertDeltaker.status.type shouldBe deltaker.status.type
        oppdatertDeltaker.sistEndretAv shouldBe navIdent

        val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).last()

        samtykke.deltakerId shouldBe deltaker.id
        samtykke.godkjent shouldBe null
        samtykke.gyldigTil shouldBe null
        sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
        samtykke.godkjentAvNav shouldBe forslag.godkjentAvNav
    }

    @Test
    fun `meldPaUtenGodkjenning - deltaker har status UTKAST - oppretter et godkjent samtykke og setter ny status for deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST))
        TestRepository.insert(deltaker)
        val godkjenningAvNav = TestData.lagGodkjenningAvNav()
        val forslag = TestData.lagForslagTilDeltaker(godkjentAvNav = godkjenningAvNav)

        deltakerService.meldPaUtenGodkjenning(deltaker, forslag, godkjenningAvNav.godkjentAv)

        val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
        oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.VENTER_PA_OPPSTART

        val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

        samtykke.deltakerId shouldBe deltaker.id
        samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
        samtykke.gyldigTil shouldBe null
        sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
        samtykke.godkjentAvNav shouldBe godkjenningAvNav
    }

    @Test
    fun `meldPaUtenGodkjenning - deltaker har et samtykke som ikke er godkjent - oppdater eksisterende samtykke`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER),
        )
        TestRepository.insert(deltaker)
        val eksisterendeSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(eksisterendeSamtykke)
        val godkjenningAvNav = TestData.lagGodkjenningAvNav()
        val forslag = TestData.lagForslagTilDeltaker(bakgrunnsinformasjon = "Nye opplysninger...", godkjentAvNav = godkjenningAvNav)

        deltakerService.meldPaUtenGodkjenning(deltaker, forslag, godkjenningAvNav.godkjentAv)

        val oppdatertDeltaker = deltakerRepository.get(deltaker.id)!!
        val samtykke = samtykkeRepository.getForDeltaker(deltaker.id).first()

        samtykke.id shouldBe eksisterendeSamtykke.id
        samtykke.deltakerId shouldBe deltaker.id
        samtykke.godkjent shouldBeCloseTo LocalDateTime.now()
        samtykke.gyldigTil shouldBe null
        sammenlignDeltakere(samtykke.deltakerVedSamtykke, oppdatertDeltaker)
        samtykke.godkjentAvNav shouldBe godkjenningAvNav
    }

    @Test
    fun `meldPaUtenGodkjenning - forslag mangler godkjenning - kaster feil`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST))
        TestRepository.insert(deltaker)
        val forslag = TestData.lagForslagTilDeltaker(godkjentAvNav = null)

        assertFailsWith<RuntimeException> {
            deltakerService.meldPaUtenGodkjenning(deltaker, forslag, TestData.randomNavIdent())
        }
    }
}
