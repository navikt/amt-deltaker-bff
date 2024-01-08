package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class DeltakerHistorikkServiceTest {
    companion object {
        private val service = DeltakerHistorikkService(
            DeltakerEndringRepository(),
            DeltakerSamtykkeRepository(),
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `getForDeltaker - ett samtykke flere endringer - returner liste riktig sortert`() {
        val deltaker = TestData.lagDeltaker()
        val samtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            godkjent = LocalDateTime.now().minusMonths(1),
        )
        val gammelEndring = TestData.lagDeltakerEndring(
            deltakerId = deltaker.id,
            endret = LocalDateTime.now().minusDays(20),
        )
        val nyEndring = TestData.lagDeltakerEndring(
            deltakerId = deltaker.id,
            endret = LocalDateTime.now().minusDays(1),
        )
        TestRepository.insert(deltaker)
        TestRepository.insert(samtykke)
        TestRepository.insert(gammelEndring)
        TestRepository.insert(nyEndring)

        val historikk = service.getForDeltaker(deltaker.id)

        historikk.size shouldBe 3
        historikk[0] shouldBe DeltakerHistorikk.Endring(nyEndring)
        historikk[1] shouldBe DeltakerHistorikk.Endring(gammelEndring)
        historikk[2] shouldBe DeltakerHistorikk.Samtykke(samtykke)
    }

    @Test
    fun `getForDeltaker - ingen endringer - returner tom liste`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        service.getForDeltaker(deltaker.id) shouldBe emptyList()
    }
}
