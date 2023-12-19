package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test

class DeltakerHistorikkRepositoryTest {
    companion object {
        lateinit var repository: DeltakerHistorikkRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = DeltakerHistorikkRepository()
        }
    }

    @Test
    fun `upsert - ny deltakerhistorikk - inserter`() {
        val deltaker = TestData.lagDeltaker()
        val deltakerHistorikk = TestData.lagDeltakerHistorikk(deltakerId = deltaker.id)
        TestRepository.insert(deltaker)

        repository.upsert(deltakerHistorikk)

        val historikkFraDb = repository.getForDeltaker(deltaker.id)
        historikkFraDb.size shouldBe 1
        sammenlignDeltakerHistorikk(historikkFraDb.first(), deltakerHistorikk)
    }

    @Test
    fun `getForDeltaker - to endringer for deltaker, navansatt og enhet finnes ikke - returnerer historikk med navident og enhetsnummer`() {
        val deltaker = TestData.lagDeltaker()
        val deltakerHistorikk = TestData.lagDeltakerHistorikk(deltakerId = deltaker.id)
        val deltakerHistorikk2 = TestData.lagDeltakerHistorikk(
            deltakerId = deltaker.id,
            endringType = DeltakerEndringType.MAL,
            endring = DeltakerEndring.EndreMal(listOf(Mal("tekst", "type", true, null))),
        )
        TestRepository.insert(deltaker)
        repository.upsert(deltakerHistorikk)
        repository.upsert(deltakerHistorikk2)

        val historikkFraDb = repository.getForDeltaker(deltaker.id)

        historikkFraDb.size shouldBe 2
        sammenlignDeltakerHistorikk(historikkFraDb.find { it.id == deltakerHistorikk.id }!!, deltakerHistorikk)
        sammenlignDeltakerHistorikk(historikkFraDb.find { it.id == deltakerHistorikk2.id }!!, deltakerHistorikk2)
    }

    @Test
    fun `getForDeltaker - to endringer for deltaker, navansatt og enhet finnes - returnerer historikk med navn for ansatt og enhet`() {
        val navAnsatt1 = TestData.lagNavAnsatt()
        TestRepository.insert(navAnsatt1)
        val navAnsatt2 = TestData.lagNavAnsatt()
        TestRepository.insert(navAnsatt2)
        val navEnhet1 = TestData.lagNavEnhet()
        TestRepository.insert(navEnhet1)
        val navEnhet2 = TestData.lagNavEnhet()
        TestRepository.insert(navEnhet2)
        val deltaker = TestData.lagDeltaker()
        val deltakerHistorikk = TestData.lagDeltakerHistorikk(
            deltakerId = deltaker.id,
            endretAv = navAnsatt1.navIdent,
            endretAvEnhet = navEnhet1.enhetsnummer,
        )
        val deltakerHistorikk2 = TestData.lagDeltakerHistorikk(
            deltakerId = deltaker.id,
            endringType = DeltakerEndringType.MAL,
            endring = DeltakerEndring.EndreMal(listOf(Mal("tekst", "type", true, null))),
            endretAv = navAnsatt2.navIdent,
            endretAvEnhet = navEnhet2.enhetsnummer,
        )
        TestRepository.insert(deltaker)
        repository.upsert(deltakerHistorikk)
        repository.upsert(deltakerHistorikk2)

        val historikkFraDb = repository.getForDeltaker(deltaker.id)

        historikkFraDb.size shouldBe 2
        sammenlignDeltakerHistorikk(historikkFraDb.find { it.id == deltakerHistorikk.id }!!, deltakerHistorikk.copy(endretAv = navAnsatt1.navn, endretAvEnhet = navEnhet1.navn))
        sammenlignDeltakerHistorikk(historikkFraDb.find { it.id == deltakerHistorikk2.id }!!, deltakerHistorikk2.copy(endretAv = navAnsatt2.navn, endretAvEnhet = navEnhet2.navn))
    }

    private fun sammenlignDeltakerHistorikk(a: DeltakerHistorikk, b: DeltakerHistorikk) {
        a.id shouldBe b.id
        a.deltakerId shouldBe b.deltakerId
        a.endringType shouldBe b.endringType
        a.endring shouldBe b.endring
        a.endretAv shouldBe b.endretAv
        a.endretAvEnhet shouldBe b.endretAvEnhet
        a.endret shouldBeCloseTo b.endret
    }
}
