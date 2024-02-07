package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.VedtakRepository
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
            VedtakRepository(),
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `getForDeltaker - ett vedtak flere endringer - returner liste riktig sortert`() {
        val deltaker = TestData.lagDeltaker()
        val vedtak = TestData.lagVedtak(
            deltakerId = deltaker.id,
            fattet = LocalDateTime.now().minusMonths(1),
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
        TestRepository.insert(vedtak)
        TestRepository.insert(gammelEndring)
        TestRepository.insert(nyEndring)

        val historikk = service.getForDeltaker(deltaker.id)

        historikk.size shouldBe 3
        sammenlignHistorikk(historikk[0], DeltakerHistorikk.Endring(nyEndring))
        sammenlignHistorikk(historikk[1], DeltakerHistorikk.Endring(gammelEndring))
        sammenlignHistorikk(historikk[2], DeltakerHistorikk.Vedtak(vedtak))
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

        is DeltakerHistorikk.Vedtak -> {
            b as DeltakerHistorikk.Vedtak
            a.vedtak.id shouldBe b.vedtak.id
            a.vedtak.deltakerId shouldBe b.vedtak.deltakerId
            a.vedtak.fattet shouldBeCloseTo b.vedtak.fattet
            a.vedtak.gyldigTil shouldBeCloseTo b.vedtak.gyldigTil
            sammenlignDeltakere(a.vedtak.deltakerVedVedtak, b.vedtak.deltakerVedVedtak)
            a.vedtak.opprettetAv shouldBe b.vedtak.opprettetAv
            a.vedtak.opprettetAvEnhet shouldBe b.vedtak.opprettetAvEnhet
            a.vedtak.opprettet shouldBeCloseTo b.vedtak.opprettet
        }
    }
}
