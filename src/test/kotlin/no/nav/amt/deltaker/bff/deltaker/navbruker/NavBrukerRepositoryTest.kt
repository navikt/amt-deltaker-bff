package no.nav.amt.deltaker.bff.deltaker.navbruker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.Adressebeskyttelse
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.BeforeClass
import org.junit.Test

class NavBrukerRepositoryTest {
    companion object {
        lateinit var repository: NavBrukerRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
            repository = NavBrukerRepository()
        }
    }

    @Test
    fun `upsert - ny bruker - inserter`() {
        val bruker = TestData.lagNavBruker()
        TestRepository.insert(TestData.lagNavAnsatt(bruker.navVeilederId!!))
        TestRepository.insert(TestData.lagNavEnhet(bruker.navEnhetId!!))
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
            adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG,
        )
        repository.upsert(oppdatertBruker).getOrNull() shouldBe oppdatertBruker
        repository.get(bruker.personId).getOrNull() shouldBe oppdatertBruker
    }
}
