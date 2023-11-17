package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

class DeltakerRepositoryTest {
    companion object {
        lateinit var repository: DeltakerRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = DeltakerRepository()
        }
    }

    @Test
    fun `upsert - ny deltaker - insertes`() {
        val deltaker = TestData.lagDeltaker()
        val deltakerliste = TestData.lagDeltakerliste(id = deltaker.deltakerlisteId)
        TestRepository.insert(deltakerliste)

        repository.upsert(deltaker)
        sammenlignDeltakere(repository.get(deltaker.id)!!, deltaker)
    }

    @Test
    fun `upsert - oppdatert deltaker - oppdaterer`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            personident = TestData.randomIdent(),
            startdato = LocalDate.now().plusWeeks(1),
            sluttdato = LocalDate.now().plusWeeks(5),
            dagerPerUke = 1F,
            deltakelsesprosent = 20F,
            sistEndretAv = TestData.randomNavIdent(),
        )

        repository.upsert(oppdatertDeltaker)
        sammenlignDeltakere(repository.get(deltaker.id)!!, oppdatertDeltaker)
    }

    @Test
    fun `upsert - ny status - inserter ny status og deaktiverer gammel`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = DeltakerStatus.Aarsak.FATT_JOBB,
            ),
        )

        repository.upsert(oppdatertDeltaker)
        sammenlignDeltakere(repository.get(deltaker.id)!!, oppdatertDeltaker)

        val statuser = repository.getDeltakerStatuser(deltaker.id)
        statuser.first { it.id == deltaker.status.id }.gyldigTil shouldNotBe null
        statuser.first { it.id == oppdatertDeltaker.status.id }.gyldigTil shouldBe null
    }

    private fun sammenlignDeltakere(a: Deltaker, b: Deltaker) {
        a.id shouldBe b.id
        a.personident shouldBe b.personident
        a.startdato shouldBe b.startdato
        a.sluttdato shouldBe b.sluttdato
        a.dagerPerUke shouldBe b.dagerPerUke
        a.deltakelsesprosent shouldBe b.deltakelsesprosent
        a.bakgrunnsinformasjon shouldBe b.bakgrunnsinformasjon
        a.mal shouldBe b.mal
        a.status.id shouldBe b.status.id
        a.status.type shouldBe b.status.type
        a.status.aarsak shouldBe b.status.aarsak
        a.status.gyldigFra shouldBeCloseTo b.status.gyldigFra
        a.status.gyldigTil shouldBeCloseTo b.status.gyldigTil
        a.status.opprettet shouldBeCloseTo b.status.opprettet
        a.sistEndretAv shouldBe b.sistEndretAv
        a.sistEndret shouldBeCloseTo b.sistEndret
        a.opprettet shouldBeCloseTo b.opprettet
    }
}
