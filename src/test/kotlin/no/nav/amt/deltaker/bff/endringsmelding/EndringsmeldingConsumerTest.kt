package no.nav.amt.deltaker.bff.endringsmelding

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBe
import org.junit.BeforeClass
import org.junit.Test

class EndringsmeldingConsumerTest {
    companion object {
        private val deltakerService = mockk<DeltakerService>()
        private val navAnsattService = mockk<NavAnsattService>()
        private val endringsmeldingService = EndringsmeldingService(
            deltakerService = deltakerService,
            navAnsattService = navAnsattService,
            endringsmeldingRepository = EndringsmeldingRepository(),
        )
        private val consumer = EndringsmeldingConsumer(endringsmeldingService)

        @BeforeClass
        @JvmStatic
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `consumeEndringsmelding - ny endringsmelding - insertes`() {
        val deltaker = TestData.lagDeltaker()
        val endringsmelding = TestData.lagEndringsmelding(deltakerId = deltaker.id)
        TestRepository.insert(deltaker)

        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        runBlocking {
            consumer.consumeEndringsmelding(endringsmelding.id, objectMapper.writeValueAsString(endringsmelding))
        }

        endringsmeldingService.get(endringsmelding.id).getOrNull() shouldBe endringsmelding
    }

    @Test
    fun `consumeEndringsmelding - tombstone - slettes`() {
        val deltaker = TestData.lagDeltaker()
        val endringsmelding = TestData.lagEndringsmelding(deltakerId = deltaker.id)
        TestRepository.insert(deltaker)
        TestRepository.insert(endringsmelding)

        every { deltakerService.get(deltaker.id) } returns Result.success(deltaker)

        runBlocking {
            consumer.consumeEndringsmelding(endringsmelding.id, null)
        }

        endringsmeldingService.get(endringsmelding.id).getOrNull() shouldBe null
    }

    @Test
    fun `consumeEndringsmelding - ny melding, deltaker finnes ikke - insertes ikke`() {
        val endringsmelding = TestData.lagEndringsmelding()

        every { deltakerService.get(endringsmelding.deltakerId) } returns Result.failure(NoSuchElementException())

        runBlocking {
            consumer.consumeEndringsmelding(endringsmelding.id, objectMapper.writeValueAsString(endringsmelding))
        }

        endringsmeldingService.get(endringsmelding.id).getOrNull() shouldBe null
    }
}
