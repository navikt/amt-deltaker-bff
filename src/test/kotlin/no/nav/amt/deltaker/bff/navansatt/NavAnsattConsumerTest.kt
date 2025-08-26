package no.nav.amt.deltaker.bff.navansatt

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.lib.ktor.clients.AmtPersonServiceClient
import no.nav.amt.lib.models.person.NavAnsatt
import no.nav.amt.lib.models.person.dto.NavAnsattDto
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class NavAnsattConsumerTest {
    private val amtPersonServiceClient = mockk<AmtPersonServiceClient>()

    companion object {
        lateinit var repository: NavAnsattRepository

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container
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

private fun NavAnsatt.toDto() = NavAnsattDto(id, navident = navIdent, navn = navn, epost = epost, telefon = telefon, null)
