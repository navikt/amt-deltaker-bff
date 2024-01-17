package no.nav.amt.deltaker.bff.navansatt

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.BeforeClass
import org.junit.Test

class NavAnsattConsumerTest {
    private val amtPersonServiceClient = mockk<AmtPersonServiceClient>()
    companion object {
        lateinit var repository: NavAnsattRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = NavAnsattRepository()
        }
    }

    @Test
    fun `consumeNavAnsatt - ny navansatt - upserter`() {
        val navAnsatt = TestData.lagNavAnsatt()
        val navAnsattConsumer = NavAnsattConsumer(NavAnsattService(repository, amtPersonServiceClient))

        runBlocking {
            navAnsattConsumer.consume(navAnsatt.id, objectMapper.writeValueAsString(navAnsatt.toDto()))
        }

        repository.get(navAnsatt.id) shouldBe navAnsatt
    }

    @Test
    fun `consumeNavAnsatt - oppdatert navansatt - upserter`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val oppdatertNavAnsatt = navAnsatt.copy(navn = "Nytt Navn")
        val navAnsattConsumer = NavAnsattConsumer(NavAnsattService(repository, amtPersonServiceClient))

        runBlocking {
            navAnsattConsumer.consume(navAnsatt.id, objectMapper.writeValueAsString(oppdatertNavAnsatt.toDto()))
        }

        repository.get(navAnsatt.id) shouldBe oppdatertNavAnsatt
    }

    @Test
    fun `consumeNavAnsatt - tombstonet navansatt - sletter`() {
        val navAnsatt = TestData.lagNavAnsatt()
        repository.upsert(navAnsatt)
        val navAnsattConsumer = NavAnsattConsumer(NavAnsattService(repository, amtPersonServiceClient))

        runBlocking {
            navAnsattConsumer.consume(navAnsatt.id, null)
        }

        repository.get(navAnsatt.id) shouldBe null
    }
}

private fun NavAnsatt.toDto() = NavAnsattDto(id, navIdent, navn)
