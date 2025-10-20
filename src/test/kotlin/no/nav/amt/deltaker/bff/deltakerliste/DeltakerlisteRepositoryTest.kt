package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.utils.data.TestData.lagArrangor
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeltakerlisteRepositoryTest {
    companion object {
        lateinit var repository: DeltakerlisteRepository

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container

            repository = DeltakerlisteRepository()
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Nested
    inner class Upsert {
        @Test
        fun `ny minimal deltakerliste - inserter`() {
            val arrangor = lagArrangor()
            TestRepository.insert(arrangor)

            val tiltakstype = lagTiltakstype()
            TestRepository.insert(tiltakstype)

            val deltakerliste = lagDeltakerliste(
                arrangor = arrangor,
                tiltakstype = tiltakstype,
            ).copy(
                status = null,
                startDato = null,
                sluttDato = null,
                oppstart = null,
                antallPlasser = null,
            )

            repository.upsert(deltakerliste)

            repository.get(deltakerliste.id).getOrNull() shouldBe deltakerliste
        }

        @Test
        fun `ny deltakerliste - inserter`() {
            val arrangor = lagArrangor()
            val deltakerliste = lagDeltakerliste(arrangor = arrangor)
            TestRepository.insert(arrangor)
            TestRepository.insert(deltakerliste.tiltak)

            repository.upsert(deltakerliste)

            repository.get(deltakerliste.id).getOrNull() shouldBe deltakerliste
        }

        @Test
        fun `deltakerliste ny sluttdato - oppdaterer`() {
            val arrangor = lagArrangor()
            val deltakerliste = lagDeltakerliste(arrangor = arrangor)
            TestRepository.insert(arrangor)
            TestRepository.insert(deltakerliste.tiltak)

            repository.upsert(deltakerliste)

            val oppdatertListe = deltakerliste.copy(sluttDato = LocalDate.now())

            repository.upsert(oppdatertListe)

            repository.get(deltakerliste.id).getOrNull() shouldBe oppdatertListe
        }
    }

    @Test
    fun `delete - sletter deltakerliste`() {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(deltakerliste.tiltak)

        repository.upsert(deltakerliste)

        repository.delete(deltakerliste.id)

        repository.get(deltakerliste.id).getOrNull() shouldBe null
    }

    @Test
    fun `get - deltakerliste og arrangor finnes - henter deltakerliste`() {
        val overordnetArrangor = lagArrangor()
        val arrangor = lagArrangor(overordnetArrangorId = overordnetArrangor.id)
        val deltakerliste = lagDeltakerliste(arrangor = arrangor, overordnetArrangor = overordnetArrangor)
        TestRepository.insert(overordnetArrangor)
        TestRepository.insert(arrangor)
        TestRepository.insert(deltakerliste.tiltak)
        repository.upsert(deltakerliste)

        val deltakerlisteMedArrangor = repository.get(deltakerliste.id).getOrThrow()

        deltakerlisteMedArrangor shouldNotBe null
        deltakerlisteMedArrangor.navn shouldBe deltakerliste.navn
        deltakerlisteMedArrangor.arrangor.arrangor.navn shouldBe arrangor.navn
        deltakerlisteMedArrangor.arrangor.overordnetArrangorNavn shouldBe overordnetArrangor.navn
    }
}
