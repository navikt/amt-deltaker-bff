package no.nav.amt.deltaker.bff.deltaker.navbruker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.DatabaseTestExtension
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.person.address.Adressebeskyttelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NavBrukerRepositoryTest {
    private val navBrukerRepository = NavBrukerRepository()

    companion object {
        @JvmField
        @RegisterExtension
        val dbExtension = DatabaseTestExtension()
    }

    @Test
    fun `upsert - ny bruker - inserter`() {
        val bruker = TestData.lagNavBruker()
        TestRepository.insert(TestData.lagNavAnsatt(bruker.navVeilederId!!))
        TestRepository.insert(TestData.lagNavEnhet(bruker.navEnhetId!!))
        navBrukerRepository.upsert(bruker).getOrNull() shouldBe bruker
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
        navBrukerRepository.upsert(oppdatertBruker).getOrNull() shouldBe oppdatertBruker
        navBrukerRepository.get(bruker.personId).getOrNull() shouldBe oppdatertBruker
    }
}
