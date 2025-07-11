package no.nav.amt.deltaker.bff.navansatt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class NavAnsattRepositoryTest {
    companion object {
        lateinit var repository: NavAnsattRepository

        @JvmStatic
        @BeforeAll
        fun setup() {
            SingletonPostgres16Container
            repository = NavAnsattRepository()
        }
    }

    @Test
    fun `getMany - flere navidenter - returnerer flere ansatte`() {
        val ansatte = listOf(
            TestData.lagNavAnsatt(),
            TestData.lagNavAnsatt(),
            TestData.lagNavAnsatt(),
        )
        ansatte.forEach { TestRepository.insert(it) }

        val faktiskeAnsatte = repository.getMany(ansatte.map { it.id })

        faktiskeAnsatte.size shouldBe ansatte.size
        faktiskeAnsatte.find { it == ansatte[0] } shouldNotBe null
        faktiskeAnsatte.find { it == ansatte[1] } shouldNotBe null
        faktiskeAnsatte.find { it == ansatte[2] } shouldNotBe null
    }

    @Test
    fun `getMany - ingen navidenter - returnerer tom list`() {
        repository.getMany(emptyList()) shouldBe emptyList()
    }
}
