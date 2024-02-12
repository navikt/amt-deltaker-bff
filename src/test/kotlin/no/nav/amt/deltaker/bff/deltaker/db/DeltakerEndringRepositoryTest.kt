package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltakerliste.Innhold
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test

class DeltakerEndringRepositoryTest {
    companion object {
        lateinit var repository: DeltakerEndringRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = DeltakerEndringRepository()
        }
    }

    @Test
    fun `upsert - ny deltakerEndring - inserter`() {
        val deltaker = TestData.lagDeltaker()
        val deltakerEndring = TestData.lagDeltakerEndring(deltakerId = deltaker.id)
        TestRepository.insert(deltaker)

        repository.upsert(deltakerEndring)

        val endringFraDb = repository.getForDeltaker(deltaker.id)
        endringFraDb.size shouldBe 1
        sammenlignDeltakerEndring(endringFraDb.first(), deltakerEndring)
    }

    @Test
    fun `getForDeltaker - to endringer for deltaker, navansatt og enhet finnes ikke - returnerer endring med navident og enhetsnummer`() {
        val deltaker = TestData.lagDeltaker()
        val deltakerEndring = TestData.lagDeltakerEndring(deltakerId = deltaker.id)
        val deltakerEndring2 = TestData.lagDeltakerEndring(
            deltakerId = deltaker.id,
            endringstype = DeltakerEndring.Endringstype.INNHOLD,
            endring = DeltakerEndring.Endring.EndreInnhold(listOf(Innhold("tekst", "type", true, null))),
        )
        TestRepository.insert(deltaker)
        repository.upsert(deltakerEndring)
        repository.upsert(deltakerEndring2)

        val endringFraDb = repository.getForDeltaker(deltaker.id)

        endringFraDb.size shouldBe 2
        sammenlignDeltakerEndring(endringFraDb.find { it.id == deltakerEndring.id }!!, deltakerEndring)
        sammenlignDeltakerEndring(endringFraDb.find { it.id == deltakerEndring2.id }!!, deltakerEndring2)
    }

    @Test
    fun `getForDeltaker - to endringer for deltaker, navansatt og enhet finnes - returnerer endring med navn for ansatt og enhet`() {
        val navAnsatt1 = TestData.lagNavAnsatt()
        TestRepository.insert(navAnsatt1)
        val navAnsatt2 = TestData.lagNavAnsatt()
        TestRepository.insert(navAnsatt2)
        val navEnhet1 = TestData.lagNavEnhet()
        TestRepository.insert(navEnhet1)
        val navEnhet2 = TestData.lagNavEnhet()
        TestRepository.insert(navEnhet2)
        val deltaker = TestData.lagDeltaker()
        val deltakerEndring = TestData.lagDeltakerEndring(
            deltakerId = deltaker.id,
            endretAv = navAnsatt1.navIdent,
            endretAvEnhet = navEnhet1.enhetsnummer,
        )
        val deltakerEndring2 = TestData.lagDeltakerEndring(
            deltakerId = deltaker.id,
            endringstype = DeltakerEndring.Endringstype.INNHOLD,
            endring = DeltakerEndring.Endring.EndreInnhold(listOf(Innhold("tekst", "type", true, null))),
            endretAv = navAnsatt2.navIdent,
            endretAvEnhet = navEnhet2.enhetsnummer,
        )
        TestRepository.insert(deltaker)
        repository.upsert(deltakerEndring)
        repository.upsert(deltakerEndring2)

        val endringFraDb = repository.getForDeltaker(deltaker.id)

        endringFraDb.size shouldBe 2
        sammenlignDeltakerEndring(
            endringFraDb.find { it.id == deltakerEndring.id }!!,
            deltakerEndring.copy(endretAv = navAnsatt1.navIdent, endretAvEnhet = navEnhet1.enhetsnummer),
        )
        sammenlignDeltakerEndring(
            endringFraDb.find { it.id == deltakerEndring2.id }!!,
            deltakerEndring2.copy(endretAv = navAnsatt2.navIdent, endretAvEnhet = navEnhet2.enhetsnummer),
        )
    }

    private fun sammenlignDeltakerEndring(a: DeltakerEndring, b: DeltakerEndring) {
        a.id shouldBe b.id
        a.deltakerId shouldBe b.deltakerId
        a.endringstype shouldBe b.endringstype
        a.endring shouldBe b.endring
        a.endretAv shouldBe b.endretAv
        a.endretAvEnhet shouldBe b.endretAvEnhet
        a.endret shouldBeCloseTo b.endret
    }
}
