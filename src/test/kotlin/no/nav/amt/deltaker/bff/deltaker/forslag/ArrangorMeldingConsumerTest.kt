package no.nav.amt.deltaker.bff.deltaker.forslag

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingConsumer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class ArrangorMeldingConsumerTest {
    companion object {
        lateinit var repository: ForslagRepository
        lateinit var service: ForslagService

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
            repository = ForslagRepository()
            service = ForslagService(repository, mockk(), mockk(), mockk())
        }
    }

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `consume - forslag VenterPaSvar - lagrer`(): Unit = runBlocking {
        val consumer = ArrangorMeldingConsumer(service)
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val forslag = TestData.lagForslag(deltakerId = deltaker.id)

        consumer.consume(
            forslag.id,
            objectMapper.writeValueAsString(forslag),
        )

        val forslagFraDb = repository.getForDeltaker(deltaker.id)
        forslagFraDb.size shouldBe 1
        forslagFraDb.first().id shouldBe forslag.id
    }

    @Test
    fun `consume - forslag tilbakekalt - sletter`(): Unit = runBlocking {
        val consumer = ArrangorMeldingConsumer(service)
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val forslag = TestData.lagForslag(deltakerId = deltaker.id)
        repository.upsert(forslag)

        consumer.consume(
            forslag.id,
            objectMapper.writeValueAsString(
                forslag.copy(
                    status = Forslag.Status.Tilbakekalt(UUID.randomUUID(), LocalDateTime.now()),
                ),
            ),
        )

        val forslagFraDb = repository.getForDeltaker(deltaker.id)
        forslagFraDb.size shouldBe 0
    }
}
