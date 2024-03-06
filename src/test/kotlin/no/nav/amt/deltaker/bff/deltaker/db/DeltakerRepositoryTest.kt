package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.deltaker.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.postgresql.util.PSQLException
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

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `upsert - ny deltaker - insertes`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker.deltakerliste)
        TestRepository.insert(deltaker.navBruker)

        repository.upsert(deltaker)
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), deltaker)
    }

    @Test
    fun `upsert - oppdatert deltaker - oppdaterer`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            startdato = LocalDate.now().plusWeeks(1),
            sluttdato = LocalDate.now().plusWeeks(5),
            dagerPerUke = 1F,
            deltakelsesprosent = 20F,
        )

        repository.upsert(oppdatertDeltaker)
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
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
                aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
            ),
        )

        repository.upsert(oppdatertDeltaker)
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)

        val statuser = repository.getDeltakerStatuser(deltaker.id)
        statuser.first { it.id == deltaker.status.id }.gyldigTil shouldNotBe null
        statuser.first { it.id == oppdatertDeltaker.status.id }.gyldigTil shouldBe null
    }

    @Test
    fun `delete - ingen endring eller vedtak - sletter deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        repository.delete(deltaker.id)

        repository.get(deltaker.id).isFailure shouldBe true
    }

    @Test
    fun `get - deltaker har flere vedtak, et aktivt - returnerer deltaker med aktivt vedtak`() {
        val baseDeltaker = TestData.lagDeltaker()
        val deltaker = TestData.leggTilHistorikk(baseDeltaker, 2)

        TestRepository.insert(deltaker)

        val vedtak = repository.get(baseDeltaker.id).getOrThrow().fattetVedtak!!
        vedtak.fattet shouldNotBe null
        vedtak.fattetAvNav shouldBe true

        val alleVedtak = deltaker.getAlleVedtak()
        alleVedtak.size shouldBe 2
        alleVedtak.find { it.gyldigTil == null }!!.id shouldBe vedtak.id
        alleVedtak.any { it.gyldigTil != null } shouldBe true
    }

    @Test
    fun `skalHaStatusDeltar - venter pa oppstart, startdato passer - returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            startdato = null,
            sluttdato = null,
        )
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = LocalDate.now().minusDays(1),
            sluttdato = LocalDate.now().plusWeeks(2),
        )
        repository.upsert(oppdatertDeltaker)

        val deltakereSomSkalHaStatusDeltar = repository.skalHaStatusDeltar()

        deltakereSomSkalHaStatusDeltar.size shouldBe 1
        deltakereSomSkalHaStatusDeltar.first().id shouldBe deltaker.id
    }

    @Test
    fun `skalHaStatusDeltar - venter pa oppstart, mangler startdato - returnerer ikke deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            startdato = null,
            sluttdato = null,
        )
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = null,
            sluttdato = null,
        )
        repository.upsert(oppdatertDeltaker)

        val deltakereSomSkalHaStatusDeltar = repository.skalHaStatusDeltar()

        deltakereSomSkalHaStatusDeltar.size shouldBe 0
    }

    @Test
    fun `skalHaAvsluttendeStatus - deltar, sluttdato passert - returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now().plusWeeks(2),
        )
        TestRepository.insert(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now().minusDays(1),
        )
        repository.upsert(oppdatertDeltaker)

        val deltakereSomSkalHaAvsluttendeStatus = repository.skalHaAvsluttendeStatus()

        deltakereSomSkalHaAvsluttendeStatus.size shouldBe 1
        deltakereSomSkalHaAvsluttendeStatus.first().id shouldBe deltaker.id
    }

    @Test
    fun `skalHaAvsluttendeStatus - venter pa oppstart, sluttdato mangler - returnerer ikke deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = null,
        )
        TestRepository.insert(deltaker)

        val deltakereSomSkalHaAvsluttendeStatus = repository.skalHaAvsluttendeStatus()

        deltakereSomSkalHaAvsluttendeStatus.size shouldBe 0
    }

    @Test
    fun `deltarPaAvsluttetDeltakerliste - deltar, dl-sluttdato passert - returnerer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now().plusDays(2),
            deltakerliste = TestData.lagDeltakerliste(status = Deltakerliste.Status.AVSLUTTET),
        )
        TestRepository.insert(deltaker)

        val deltakerePaAvsluttetDeltakerliste = repository.deltarPaAvsluttetDeltakerliste()

        deltakerePaAvsluttetDeltakerliste.size shouldBe 1
        deltakerePaAvsluttetDeltakerliste.first().id shouldBe deltaker.id
    }

    @Test
    fun `deltarPaAvsluttetDeltakerliste - har sluttet, dl-sluttdato passert - returnerer ikke deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now(),
            deltakerliste = TestData.lagDeltakerliste(status = Deltakerliste.Status.AVSLUTTET),
        )
        TestRepository.insert(deltaker)

        val deltakerePaAvsluttetDeltakerliste = repository.deltarPaAvsluttetDeltakerliste()

        deltakerePaAvsluttetDeltakerliste.size shouldBe 0
    }

    @Test
    fun `create - ny kladd - oppretter ny deltaker`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker.navBruker)
        TestRepository.insert(deltaker.deltakerliste)

        val kladd = deltaker.toKladdResponse()
        repository.create(kladd)

        sammenlignDeltakere(deltaker, repository.get(kladd.id).getOrThrow())
    }

    @Test
    fun `create - deltaker eksisterer - feiler`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)

        val kladd = deltaker.toKladdResponse()

        assertThrows(PSQLException::class.java) {
            repository.create(kladd)
        }
    }

    @Test
    fun `update - deltaker er endret - oppdaterer`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("ny bakgrunn for innsøk")
        val oppdatertDeltaker = TestData.leggTilHistorikk(
            deltaker = deltaker.copy(bakgrunnsinformasjon = endring.bakgrunnsinformasjon),
            endringer = listOf(TestData.lagDeltakerEndring(endring = endring)),
        )

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `update - deltakerstatus er endret - oppdaterer`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
        )
        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `update - deltakerstatus er ikke endret - oppdaterer ikke status`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        repository.update(deltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), deltaker)
    }
}

private fun Deltaker.toDeltakeroppdatering() = Deltakeroppdatering(
    id, startdato, sluttdato, dagerPerUke, deltakelsesprosent, bakgrunnsinformasjon, innhold, status, historikk,
)

fun sammenlignDeltakere(a: Deltaker, b: Deltaker) {
    a.id shouldBe b.id
    a.navBruker shouldBe b.navBruker
    a.startdato shouldBe b.startdato
    a.sluttdato shouldBe b.sluttdato
    a.dagerPerUke shouldBe b.dagerPerUke
    a.deltakelsesprosent shouldBe b.deltakelsesprosent
    a.bakgrunnsinformasjon shouldBe b.bakgrunnsinformasjon
    a.innhold shouldBe b.innhold
    a.historikk shouldBe b.historikk
    a.status.id shouldBe b.status.id
    a.status.type shouldBe b.status.type
    a.status.aarsak shouldBe b.status.aarsak
    a.status.gyldigFra shouldBeCloseTo b.status.gyldigFra
    a.status.gyldigTil shouldBeCloseTo b.status.gyldigTil
    a.status.opprettet shouldBeCloseTo b.status.opprettet
}

private fun Deltaker.toKladdResponse() = KladdResponse(
    id = id,
    navBruker = navBruker,
    deltakerlisteId = deltakerliste.id,
    startdato = startdato,
    sluttdato = sluttdato,
    dagerPerUke = dagerPerUke,
    deltakelsesprosent = deltakelsesprosent,
    bakgrunnsinformasjon = bakgrunnsinformasjon,
    innhold = innhold,
    status = status,
)

private fun Deltaker.getAlleVedtak() = historikk.filterIsInstance<DeltakerHistorikk.Vedtak>().map { it.vedtak }
