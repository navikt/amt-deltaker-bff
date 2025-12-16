package no.nav.amt.deltaker.bff.arrangor

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.utils.TestData.lagArrangor
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArrangorConsumerTest {
    companion object {
        lateinit var repository: ArrangorRepository

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container

            repository = ArrangorRepository()
        }
    }

    @Test
    fun `consumeArrangor - ny arrangor - upserter`() {
        val arrangor = lagArrangor()
        val arrangorConsumer = ArrangorConsumer(repository)

        runBlocking {
            arrangorConsumer.consume(arrangor.id, objectMapper.writeValueAsString(arrangor))
        }

        repository.get(arrangor.id) shouldBe arrangor
    }

    @Test
    fun `consumeArrangor - oppdatert arrangor - upserter`() {
        val arrangor = lagArrangor()
        repository.upsert(arrangor)

        val oppdatertArrangor = arrangor.copy(navn = "Oppdatert Arrangor")

        val arrangorConsumer = ArrangorConsumer(repository)

        runBlocking {
            arrangorConsumer.consume(arrangor.id, objectMapper.writeValueAsString(oppdatertArrangor))
        }

        repository.get(arrangor.id) shouldBe oppdatertArrangor
    }

    @Test
    fun `consumeArrangor - tombstonet arrangor - sletter`() {
        val arrangor = lagArrangor()
        repository.upsert(arrangor)

        val arrangorConsumer = ArrangorConsumer(repository)

        runBlocking {
            arrangorConsumer.consume(arrangor.id, null)
        }

        repository.get(arrangor.id) shouldBe null
    }
}
