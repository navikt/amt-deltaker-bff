package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltaker.model.VedtakDbo
import no.nav.amt.deltaker.bff.deltaker.sammenlignDeltakereVedVedtak
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class VedtakRepositoryTest {
    companion object {
        lateinit var repository: VedtakRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = VedtakRepository()
        }
    }

    @Test
    fun `upsert - nytt vedtak - inserter`() {
        val deltaker = TestData.lagDeltaker()
        val vedtak: Vedtak = TestData.lagVedtak(deltakerVedVedtak = deltaker)

        TestRepository.insert(deltaker)
        repository.upsert(vedtak.toDbo(deltaker))

        sammenlignVedtak(repository.get(vedtak.id)!!, vedtak)
    }

    @Test
    fun `upsert - oppdatert vedtak - oppdaterer`() {
        val deltaker = TestData.lagDeltaker()
        val vedtak: Vedtak = TestData.lagVedtak(deltakerVedVedtak = deltaker)

        TestRepository.insert(deltaker)
        repository.upsert(vedtak.toDbo(deltaker))

        val oppdatertVedtak = vedtak.copy(fattet = LocalDateTime.now())
        repository.upsert(oppdatertVedtak.toDbo(deltaker))

        sammenlignVedtak(repository.get(vedtak.id)!!, oppdatertVedtak)
    }

    @Test
    fun `upsert - vedtak fattet av nav - inserter`() {
        val deltaker = TestData.lagDeltaker()
        val vedtak: Vedtak = TestData.lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = LocalDateTime.now(),
            fattetAvNav = TestData.lagFattetAvNav(),
        )

        TestRepository.insert(deltaker)
        repository.upsert(vedtak.toDbo(deltaker))

        sammenlignVedtak(repository.get(vedtak.id)!!, vedtak)
    }

    @Test
    fun `getIkkeFattet - flere vedtak - henter det som ikke er fattet`() {
        val deltaker = TestData.lagDeltaker()
        val fattet: Vedtak = TestData.lagVedtak(
            deltakerId = deltaker.id,
            fattet = LocalDateTime.now().minusMonths(2),
            deltakerVedVedtak = deltaker,
        )
        val ikkeFattet: Vedtak = TestData.lagVedtak(
            deltakerId = deltaker.id,
            deltakerVedVedtak = deltaker,
        )
        TestRepository.insert(deltaker)
        TestRepository.insert(fattet)
        TestRepository.insert(ikkeFattet)

        sammenlignVedtak(repository.getIkkeFattet(deltaker.id)!!, ikkeFattet)
    }

    @Test
    fun `getIkkeFattet - fattet vedtak - returnerer null`() {
        val deltaker = TestData.lagDeltaker()
        val fattet: Vedtak = TestData.lagVedtak(
            deltakerId = deltaker.id,
            fattet = LocalDateTime.now().minusMonths(2),
            deltakerVedVedtak = deltaker,
        )
        TestRepository.insert(deltaker)
        TestRepository.insert(fattet)

        repository.getIkkeFattet(deltaker.id) shouldBe null
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

fun Vedtak.toDbo(deltaker: Deltaker) = VedtakDbo(
    id,
    deltakerId,
    fattet,
    gyldigTil,
    deltaker,
    fattetAvNav,
    opprettet,
    opprettetAv,
    opprettetAvEnhet,
    sistEndret,
    sistEndretAv,
    sistEndretAvEnhet,
)
