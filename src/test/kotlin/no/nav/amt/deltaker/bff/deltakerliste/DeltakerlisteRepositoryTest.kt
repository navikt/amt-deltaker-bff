package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

class DeltakerlisteRepositoryTest {
    companion object {
        lateinit var repository: DeltakerlisteRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = DeltakerlisteRepository()
        }
    }

    @Test
    fun `upsert - ny deltakerliste - inserter`() {
        val deltakerliste = TestData.lagDeltakerliste()
        TestRepository.insert(TestData.lagArrangor(id = deltakerliste.arrangorId))

        repository.upsert(deltakerliste)

        repository.get(deltakerliste.id) shouldBe deltakerliste
    }

    @Test
    fun `upsert - deltakerliste ny sluttdato - oppdaterer`() {
        val deltakerliste = TestData.lagDeltakerliste()
        TestRepository.insert(TestData.lagArrangor(id = deltakerliste.arrangorId))

        repository.upsert(deltakerliste)

        val oppdatertListe = deltakerliste.copy(sluttDato = LocalDate.now())

        repository.upsert(oppdatertListe)

        repository.get(deltakerliste.id) shouldBe oppdatertListe
    }

    @Test
    fun `delete - sletter deltakerliste`() {
        val deltakerliste = TestData.lagDeltakerliste()
        TestRepository.insert(TestData.lagArrangor(id = deltakerliste.arrangorId))

        repository.upsert(deltakerliste)

        repository.delete(deltakerliste.id)

        repository.get(deltakerliste.id) shouldBe null
    }

    @Test
    fun `getDeltakerlisteMedArrangor - deltakerliste og arrangor finnes - henter deltakerliste og arrangor`() {
        val deltakerliste = TestData.lagDeltakerliste()
        val arrangor = TestData.lagArrangor(id = deltakerliste.arrangorId)
        TestRepository.insert(arrangor)
        repository.upsert(deltakerliste)

        val deltakerlisteMedArrangor = repository.getDeltakerlisteMedArrangor(deltakerliste.id)

        deltakerlisteMedArrangor shouldNotBe null
        deltakerlisteMedArrangor?.deltakerliste?.navn shouldBe deltakerliste.navn
        deltakerlisteMedArrangor?.arrangor?.navn shouldBe arrangor.navn
    }
}
