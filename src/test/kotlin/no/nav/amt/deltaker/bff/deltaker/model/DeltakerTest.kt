package no.nav.amt.deltaker.bff.deltaker.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.sammenlignDeltakereVedVedtak
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Test
import java.time.LocalDateTime

class DeltakerTest {
    @Test
    fun `getDeltakerHistorikkSortert - ett vedtak flere endringer - returner liste riktig sortert`() {
        val baseDeltaker = TestData.lagDeltaker(historikk = false)
        val vedtak = TestData.lagVedtak(
            deltakerId = baseDeltaker.id,
            fattet = LocalDateTime.now().minusMonths(1),
            sistEndret = LocalDateTime.now().minusMonths(1),
        )
        val gammelEndring = TestData.lagDeltakerEndring(
            deltakerId = baseDeltaker.id,
            endret = LocalDateTime.now().minusDays(20),
        )
        val nyEndring = TestData.lagDeltakerEndring(
            deltakerId = baseDeltaker.id,
            endret = LocalDateTime.now().minusDays(1),
        )
        val deltaker = TestData.leggTilHistorikk(baseDeltaker, listOf(vedtak), listOf(gammelEndring, nyEndring))

        val historikk = deltaker.getDeltakerHistorikSortert()

        historikk.size shouldBe 3
        sammenlignHistorikk(historikk[0], DeltakerHistorikk.Endring(nyEndring))
        sammenlignHistorikk(historikk[1], DeltakerHistorikk.Endring(gammelEndring))
        sammenlignHistorikk(historikk[2], DeltakerHistorikk.Vedtak(vedtak))
    }

    @Test
    fun `getDeltakerHistorikkSortert - ingen historikk - returner tom liste`() {
        val deltaker = TestData.lagDeltaker(historikk = false)
        deltaker.getDeltakerHistorikSortert() shouldBe emptyList()
    }

    @Test
    fun `fattetVedtak - flere vedtak - henter vedtaket som er gyldig og fattet`() {
        val deltaker = TestData.lagDeltaker(historikk = false)
        val fattet = TestData.lagVedtak(
            deltakerId = deltaker.id,
            fattet = LocalDateTime.now().minusMonths(2),
            deltakerVedVedtak = deltaker,
            gyldigTil = LocalDateTime.now().minusMonths(1),
        )
        val fattet2 = TestData.lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = LocalDateTime.now().minusMonths(1),
            gyldigTil = null,
        )

        val deltakerMedVedtak = TestData.leggTilHistorikk(deltaker, listOf(fattet, fattet2))

        sammenlignVedtak(deltakerMedVedtak.fattetVedtak!!, fattet2)
    }

    @Test
    fun `fattetVedtak - ingen fattet vedtak - returnere null`() {
        val deltaker = TestData.lagDeltaker(historikk = false)

        deltaker.fattetVedtak shouldBe null
    }

    @Test
    fun `getIkkeFattetVedtak - deltaker har ikke fattet vedtak - returnerer vedtak`() {
        val deltaker = TestData.lagDeltaker(historikk = false)
        val vedtak = TestData.lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = null,
        )
        val deltakerMedVedtak = TestData.leggTilHistorikk(deltaker, listOf(vedtak))
        sammenlignVedtak(deltakerMedVedtak.ikkeFattetVedtak!!, vedtak)
    }

    @Test
    fun `getIkkeFattetVedtak - deltaker har kun fattet vedtak - returnerer null`() {
        val deltaker = TestData.lagDeltaker(historikk = false)
        val fattet = TestData.lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = LocalDateTime.now().minusMonths(2),
        )
        val deltakerMedVedtak = TestData.leggTilHistorikk(deltaker, listOf(fattet))
        deltakerMedVedtak.ikkeFattetVedtak shouldBe null
    }
}

fun sammenlignHistorikk(a: DeltakerHistorikk, b: DeltakerHistorikk) {
    when (a) {
        is DeltakerHistorikk.Endring -> {
            b as DeltakerHistorikk.Endring
            a.endring.id shouldBe b.endring.id
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
            sammenlignDeltakereVedVedtak(a.vedtak.deltakerVedVedtak, b.vedtak.deltakerVedVedtak)
            a.vedtak.opprettetAv shouldBe b.vedtak.opprettetAv
            a.vedtak.opprettetAvEnhet shouldBe b.vedtak.opprettetAvEnhet
            a.vedtak.opprettet shouldBeCloseTo b.vedtak.opprettet
        }
    }
}

fun sammenlignVedtak(a: Vedtak, b: Vedtak) {
    a.id shouldBe b.id
    a.deltakerId shouldBe b.deltakerId
    a.fattet shouldBeCloseTo b.fattet
    a.gyldigTil shouldBeCloseTo b.gyldigTil
    sammenlignDeltakereVedVedtak(a.deltakerVedVedtak, b.deltakerVedVedtak)
    a.fattetAvNav shouldBe b.fattetAvNav
    a.opprettet shouldBeCloseTo b.opprettet
    a.opprettetAv shouldBe b.opprettetAv
    a.opprettetAvEnhet shouldBe b.opprettetAvEnhet
    a.sistEndret shouldBeCloseTo b.sistEndret
    a.sistEndretAv shouldBe b.sistEndretAv
    a.sistEndretAvEnhet shouldBe b.sistEndretAvEnhet
}
