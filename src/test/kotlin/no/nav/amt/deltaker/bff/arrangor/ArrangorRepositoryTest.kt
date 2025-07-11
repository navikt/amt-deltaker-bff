package no.nav.amt.deltaker.bff.arrangor

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArrangorRepositoryTest {
    companion object {
        lateinit var repository: ArrangorRepository

        @JvmStatic
        @BeforeAll
        fun setup() {
            SingletonPostgres16Container
            repository = ArrangorRepository()
        }
    }

    @Test
    fun `upsert - ny arrangor - inserter`() {
        val arrangor = TestData.lagArrangor()
        repository.upsert(arrangor)

        repository.get(arrangor.id) shouldBe arrangor
    }

    @Test
    fun `upsert - eksisterende arrangor - oppdaterer`() {
        val arrangor = TestData.lagArrangor()
        repository.upsert(arrangor)

        val oppdatertArrangor = arrangor.copy(navn = "Oppdatert Arrangor")
        repository.upsert(oppdatertArrangor)

        repository.get(arrangor.id) shouldBe oppdatertArrangor
    }

    @Test
    fun `delete - eksisterende arrangor - sletter`() {
        val arrangor = TestData.lagArrangor()
        repository.upsert(arrangor)

        repository.delete(arrangor.id)

        repository.get(arrangor.id) shouldBe null
    }
}
