package no.nav.amt.deltaker.bff.arrangor

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.BeforeClass
import org.junit.Test

class ArrangorRepositoryTest {

    companion object {
        lateinit var repository: ArrangorRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
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
}
