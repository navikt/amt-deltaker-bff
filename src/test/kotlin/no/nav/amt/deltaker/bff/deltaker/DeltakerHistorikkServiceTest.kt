package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
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
        sammenlignHistorikk(historikk[0], DeltakerHistorikk.Endring(nyEndring))
        sammenlignHistorikk(historikk[1], DeltakerHistorikk.Endring(gammelEndring))
        sammenlignHistorikk(historikk[2], DeltakerHistorikk.Samtykke(samtykke))
    }

    @Test
    fun `getForDeltaker - ingen endringer - returner tom liste`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        service.getForDeltaker(deltaker.id) shouldBe emptyList()
    }
}

fun sammenlignHistorikk(a: DeltakerHistorikk, b: DeltakerHistorikk) {
    when (a) {
        is DeltakerHistorikk.Endring -> {
            b as DeltakerHistorikk.Endring
            a.endring.id shouldBe b.endring.id
            a.endring.endringstype shouldBe b.endring.endringstype
            a.endring.endring shouldBe b.endring.endring
            a.endring.endretAv shouldBe b.endring.endretAv
            a.endring.endretAvEnhet shouldBe b.endring.endretAvEnhet
            a.endring.endret shouldBeCloseTo b.endring.endret
        }

        is DeltakerHistorikk.Samtykke -> {
            b as DeltakerHistorikk.Samtykke
            a.samtykke.id shouldBe b.samtykke.id
            a.samtykke.deltakerId shouldBe b.samtykke.deltakerId
            a.samtykke.godkjent shouldBeCloseTo b.samtykke.godkjent
            a.samtykke.gyldigTil shouldBeCloseTo b.samtykke.gyldigTil
            sammenlignDeltakere(a.samtykke.deltakerVedSamtykke, b.samtykke.deltakerVedSamtykke)
            a.samtykke.opprettetAv shouldBe b.samtykke.opprettetAv
            a.samtykke.opprettetAvEnhet shouldBe b.samtykke.opprettetAvEnhet
            a.samtykke.opprettet shouldBeCloseTo b.samtykke.opprettet
        }
    }
}
