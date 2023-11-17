package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

class DeltakerServiceTest {
    private val personident = "12345678910"
    private val opprettetAv = "OpprettetAv"
    companion object {
        lateinit var deltakerlisteRepository: DeltakerlisteRepository
        lateinit var deltakerRepository: DeltakerRepository
        lateinit var deltakerService: DeltakerService

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            deltakerlisteRepository = DeltakerlisteRepository()
            deltakerRepository = DeltakerRepository()
            deltakerService = DeltakerService(deltakerRepository, deltakerlisteRepository)
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
}
