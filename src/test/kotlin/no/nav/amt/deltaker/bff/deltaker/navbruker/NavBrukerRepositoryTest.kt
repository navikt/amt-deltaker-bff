package no.nav.amt.deltaker.bff.deltaker.navbruker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.BeforeClass
import org.junit.Test

class NavBrukerRepositoryTest {
    companion object {
        lateinit var repository: NavBrukerRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = NavBrukerRepository()
        }
    }

    @Test
    fun `upsert - ny bruker - inserter`() {
        val bruker = TestData.lagNavBruker()
        repository.upsert(bruker).getOrNull() shouldBe bruker
    }

    @Test
    fun `upsert - oppdatert bruker - oppdaterer`() {
        val bruker = TestData.lagNavBruker()
        TestRepository.insert(bruker)

        val oppdatertBruker = bruker.copy(
            personident = TestData.randomIdent(),
            fornavn = "Nytt Fornavn",
            mellomnavn = null,
            etternavn = "Nytt Etternavn",
        )
        repository.upsert(oppdatertBruker).getOrNull() shouldBe oppdatertBruker
        repository.get(bruker.personId).getOrNull() shouldBe oppdatertBruker
    }
}
