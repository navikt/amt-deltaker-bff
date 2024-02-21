package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.Before
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

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `upsert - ny deltakerliste - inserter`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)

        repository.upsert(deltakerliste)

        repository.get(deltakerliste.id).getOrNull() shouldBe deltakerliste
    }

    @Test
    fun `upsert - deltakerliste ny sluttdato - oppdaterer`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)

        repository.upsert(deltakerliste)

        val oppdatertListe = deltakerliste.copy(sluttDato = LocalDate.now())

        repository.upsert(oppdatertListe)

        repository.get(deltakerliste.id).getOrNull() shouldBe oppdatertListe
    }

    @Test
    fun `delete - sletter deltakerliste`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)

        repository.upsert(deltakerliste)

        repository.delete(deltakerliste.id)

        repository.get(deltakerliste.id).getOrNull() shouldBe null
    }

    @Test
    fun `get - deltakerliste og arrangor finnes - henter deltakerliste`() {
        val overordnetArrangor = TestData.lagArrangor()
        val arrangor = TestData.lagArrangor(overordnetArrangorId = overordnetArrangor.id)
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor, overordnetArrangor = overordnetArrangor)
        TestRepository.insert(overordnetArrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)
        repository.upsert(deltakerliste)

        val deltakerlisteMedArrangor = repository.get(deltakerliste.id).getOrThrow()

        deltakerlisteMedArrangor shouldNotBe null
        deltakerlisteMedArrangor.navn shouldBe deltakerliste.navn
        deltakerlisteMedArrangor.arrangor.arrangor.navn shouldBe arrangor.navn
        deltakerlisteMedArrangor.arrangor.overordnetArrangorNavn shouldBe overordnetArrangor.navn
    }
}
