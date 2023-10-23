package no.nav.amt.deltaker.bff.arrangor

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.BeforeClass
import org.junit.Test

class ArrangorConsumerTest {

    companion object {
        val repository = ArrangorRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `consumeArrangor - ny arrangor - upserter`() {
        val arrangor = TestData.lagArrangor()
        consumeArrangor(arrangor.id, objectMapper().writeValueAsString(arrangor))

        repository.get(arrangor.id) shouldBe arrangor
    }

    @Test
    fun `consumeArrangor - oppdatert arrangor - upserter`() {
        val arrangor = TestData.lagArrangor()
        repository.upsert(arrangor)

        val oppdatertArrangor = arrangor.copy(navn = "Oppdatert Arrangor")

        consumeArrangor(arrangor.id, objectMapper().writeValueAsString(oppdatertArrangor))

        repository.get(arrangor.id) shouldBe oppdatertArrangor
    }

    @Test
    fun `consumeArrangor - tombstonet arrangor - sletter`() {
        val arrangor = TestData.lagArrangor()
        repository.upsert(arrangor)

        consumeArrangor(arrangor.id, null)

        repository.get(arrangor.id) shouldBe null
    }
}
