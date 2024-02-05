package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class DeltakerSamtykkeRepositoryTest {
    companion object {
        lateinit var repository: DeltakerSamtykkeRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = DeltakerSamtykkeRepository()
        }
    }

    @Test
    fun `upsert - nytt samtykke - inserter`() {
        val samtykke: DeltakerSamtykke = TestData.lagDeltakerSamtykke()

        TestRepository.insert(samtykke.deltakerVedSamtykke)
        repository.upsert(samtykke)

        sammenlignSamtykker(repository.get(samtykke.id)!!, samtykke)
    }

    @Test
    fun `upsert - oppdatert samtykke - oppdaterer`() {
        val samtykke: DeltakerSamtykke = TestData.lagDeltakerSamtykke()

        TestRepository.insert(samtykke.deltakerVedSamtykke)
        repository.upsert(samtykke)

        val oppdatertSamtykke = samtykke.copy(godkjent = LocalDateTime.now())
        repository.upsert(oppdatertSamtykke)

        sammenlignSamtykker(repository.get(samtykke.id)!!, oppdatertSamtykke)
    }

    @Test
    fun `upsert - samtykke med godkjenning av nav - inserter`() {
        val samtykke: DeltakerSamtykke = TestData.lagDeltakerSamtykke(
            godkjent = LocalDateTime.now(),
            godkjentAvNav = TestData.lagGodkjenningAvNav(),
        )

        TestRepository.insert(samtykke.deltakerVedSamtykke)
        repository.upsert(samtykke)

        sammenlignSamtykker(repository.get(samtykke.id)!!, samtykke)
    }

    @Test
    fun `getIkkeGodkjent - flere samtykker - henter det som ikke er godkjent`() {
        val deltaker = TestData.lagDeltaker()
        val godkjent: DeltakerSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            godkjent = LocalDateTime.now().minusMonths(2),
            deltakerVedSamtykke = deltaker,
        )
        val ikkeGodkjent: DeltakerSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(deltaker)
        TestRepository.insert(godkjent)
        TestRepository.insert(ikkeGodkjent)

        sammenlignSamtykker(repository.getIkkeGodkjent(deltaker.id)!!, ikkeGodkjent)
    }

    @Test
    fun `getIkkeGodkjent - godkjent samtykke - returnerer null`() {
        val deltaker = TestData.lagDeltaker()
        val godkjent: DeltakerSamtykke = TestData.lagDeltakerSamtykke(
            deltakerId = deltaker.id,
            godkjent = LocalDateTime.now().minusMonths(2),
            deltakerVedSamtykke = deltaker,
        )
        TestRepository.insert(deltaker)
        TestRepository.insert(godkjent)

        repository.getIkkeGodkjent(deltaker.id) shouldBe null
    }
}

fun sammenlignSamtykker(a: DeltakerSamtykke, b: DeltakerSamtykke) {
    a.id shouldBe b.id
    a.deltakerId shouldBe b.deltakerId
    a.godkjent shouldBeCloseTo b.godkjent
    a.gyldigTil shouldBeCloseTo b.gyldigTil
    sammenlignDeltakere(a.deltakerVedSamtykke, b.deltakerVedSamtykke)
    a.godkjentAvNav shouldBe b.godkjentAvNav
    a.opprettet shouldBeCloseTo b.opprettet
    a.opprettetAv shouldBe b.opprettetAv
    a.opprettetAvEnhet shouldBe b.opprettetAvEnhet
    a.sistEndret shouldBeCloseTo b.sistEndret
    a.sistEndretAv shouldBe b.sistEndretAv
    a.sistEndretAvEnhet shouldBe b.sistEndretAvEnhet
}
