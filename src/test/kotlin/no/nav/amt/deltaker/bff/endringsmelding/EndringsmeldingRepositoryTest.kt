package no.nav.amt.deltaker.bff.endringsmelding

import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBe
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class EndringsmeldingRepositoryTest {

    companion object {
        private val repository = EndringsmeldingRepository()

        @BeforeClass
        @JvmStatic
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `upsert - ny endringsmelding - inserter`() {
        val endringsmelding = TestData.lagEndringsmelding()
        TestRepository.insert(TestData.lagDeltaker(endringsmelding.deltakerId))
        repository.upsert(endringsmelding)
        repository.get(endringsmelding.id).getOrNull() shouldBe endringsmelding
    }

    @Test
    fun `upsert - endret endringsmelding - oppdaterer`() {
        val endringsmelding = TestData.lagEndringsmelding()
        TestRepository.insert(endringsmelding)
        val oppdatertEndringsmelding = endringsmelding.copy(
            status = Endringsmelding.Status.UTFORT,
            utfortTidspunkt = LocalDateTime.now(),
            utfortAvNavAnsattId = UUID.randomUUID(),
        )
        repository.upsert(oppdatertEndringsmelding)
        repository.get(endringsmelding.id).getOrNull() shouldBe oppdatertEndringsmelding
    }

    @Test
    fun `delete - sletter endringsmelding`() {
        val endringsmelding = TestData.lagEndringsmelding()
        TestRepository.insert(endringsmelding)
        repository.delete(endringsmelding.id)
        repository.get(endringsmelding.id).getOrNull() shouldBe null
    }
}
