package no.nav.amt.deltaker.bff.deltakerliste.kafka

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtArrangorClient
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

class DeltakerlisteConsumerTest {

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
    fun `consumeDeltakerliste - ny liste og arrangor - lagrer deltakerliste`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))
        val consumer = DeltakerlisteConsumer(repository, arrangorService)

        runBlocking {
            consumer.consumeDeltakerliste(
                deltakerliste.id,
                objectMapper.writeValueAsString(TestData.lagDeltakerlisteDto(arrangor, deltakerliste)),
            )

            repository.get(deltakerliste.id) shouldBe deltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - ny sluttdato - oppdaterer deltakerliste`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(repository, arrangorService)

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now())

        runBlocking {
            consumer.consumeDeltakerliste(
                deltakerliste.id,
                objectMapper.writeValueAsString(TestData.lagDeltakerlisteDto(arrangor, oppdatertDeltakerliste)),
            )

            repository.get(deltakerliste.id) shouldBe oppdatertDeltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - tombstone - sletter deltakerliste`() {
        val deltakerliste = TestData.lagDeltakerliste()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())

        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(repository, arrangorService)

        runBlocking {
            consumer.consumeDeltakerliste(deltakerliste.id, null)

            repository.get(deltakerliste.id) shouldBe null
        }
    }
}
