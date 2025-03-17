package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.data.endre
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.postgresql.util.PSQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class DeltakerRepositoryTest {
    companion object {
        lateinit var repository: DeltakerRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
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

        val json = objectMapper.writePolymorphicListAsString(deltaker.historikk)
        val historikk = objectMapper.readValue<List<DeltakerHistorikk>>(json)

        val vedtak = repository.get(baseDeltaker.id).getOrThrow().fattetVedtak!!
        vedtak.fattet shouldNotBe null
        vedtak.fattetAvNav shouldBe true

        val alleVedtak = deltaker.getAlleVedtak()
        alleVedtak.size shouldBe 2
        alleVedtak.find { it.gyldigTil == null }!!.id shouldBe vedtak.id
        alleVedtak.any { it.gyldigTil != null } shouldBe true
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
        val sistEndret = LocalDateTime.now().minusDays(3)
        val deltaker = TestData.lagDeltaker(sistEndret = sistEndret)
        TestRepository.insert(deltaker)
        val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("ny bakgrunn for innsøk")
        val oppdatertDeltaker = deltaker.endre(TestData.lagDeltakerEndring(endring = endring))

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `update - deltaker endringshistorikk mangler - oppdaterer ikke`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = deltaker.copy(bakgrunnsinformasjon = "Endringshistorikk mangler")

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())

        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), deltaker)
    }

    @Test
    fun `update - deltaker endringshistorikk mangler men er utkast - oppdaterer`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = deltaker.copy(bakgrunnsinformasjon = "Endringshistorikk mangler")

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `update - deltakerstatus er endret - oppdaterer`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)
        val oppdatertDeltaker = deltaker.copy(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `update - deltaker kan ikke endres - oppdaterer ikke`() {
        val sistEndret = LocalDateTime.now().minusDays(3)
        val deltaker = TestData.lagDeltaker(sistEndret = sistEndret, kanEndres = false)
        TestRepository.insert(deltaker)
        val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("ny bakgrunn for innsøk")
        val oppdatertDeltaker = deltaker.endre(TestData.lagDeltakerEndring(endring = endring))

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), deltaker)
    }

    @Test
    fun `update - deltaker kan ikke endres, kun oppdatert historikk - oppdaterer historikk`() {
        val sistEndret = LocalDateTime.now().minusDays(3)
        val deltaker = TestData.lagDeltaker(sistEndret = sistEndret, kanEndres = false)
        TestRepository.insert(deltaker)
        val avvistForslag = TestData.lagForslag(
            deltakerId = deltaker.id,
            status = Forslag.Status.Avvist(
                avvistAv = Forslag.NavAnsatt(id = UUID.randomUUID(), enhetId = UUID.randomUUID()),
                avvist = LocalDateTime.now(),
                begrunnelseFraNav = "begrunnelse",
            ),
        )
        val historikk = deltaker.historikk + listOf(DeltakerHistorikk.Forslag(avvistForslag))
        val oppdatertDeltaker = deltaker.copy(historikk = historikk, sistEndret = LocalDateTime.now())

        repository.update(oppdatertDeltaker.toDeltakeroppdatering())
        sammenlignDeltakere(repository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
    }

    @Test
    fun `getTidligereAvsluttedeDeltakelser - har ingen tidligere deltakelse - returnerer tom liste`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        repository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe emptyList()
    }

    @Test
    fun `getTidligereAvsluttedeDeltakelser - har aktiv tidligere deltakelse - returnerer tom liste`() {
        val avsluttetDeltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(avsluttetDeltaker)
        val deltaker = TestData.lagDeltaker(
            deltakerliste = avsluttetDeltaker.deltakerliste,
            navBruker = avsluttetDeltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        repository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe emptyList()
    }

    @Test
    fun `getTidligereAvsluttedeDeltakelser - har tidligere avsluttet deltakelse - returnerer id`() {
        val avsluttetDeltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(avsluttetDeltaker)
        val deltaker = TestData.lagDeltaker(
            deltakerliste = avsluttetDeltaker.deltakerliste,
            navBruker = avsluttetDeltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        )
        TestRepository.insert(deltaker)

        repository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe listOf(avsluttetDeltaker.id)
    }

    @Test
    fun `getTidligereAvsluttedeDeltakelser - har tidligere avsluttet deltakelse, er avsluttet deltakelse - returnerer id`() {
        val avsluttetDeltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(avsluttetDeltaker)
        val deltaker = TestData.lagDeltaker(
            deltakerliste = avsluttetDeltaker.deltakerliste,
            navBruker = avsluttetDeltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(deltaker)

        repository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe listOf(avsluttetDeltaker.id)
    }

    @Test
    fun `getUtdaterteKladder - finnes en utdatert kladd - returnerer utdatert kladd`() {
        val kladd = TestData.lagDeltakerKladd(sistEndret = LocalDateTime.now().minusDays(2))
        TestRepository.insert(kladd)
        val utdatertKladd = TestData.lagDeltakerKladd(sistEndret = LocalDateTime.now().minusDays(20))
        TestRepository.insert(utdatertKladd)

        val utdaterteKladder = repository.getUtdaterteKladder(LocalDateTime.now().minusWeeks(2))

        utdaterteKladder.size shouldBe 1
        utdaterteKladder.first().id shouldBe utdatertKladd.id
    }

    @Test
    fun `oppdaterSistBesokt - oppdaterer sistBesokt - skal ikke feile`() {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val sistBesokt = ZonedDateTime.now()
        repository.oppdaterSistBesokt(deltaker.id, sistBesokt)

        val lagretSistBesokt = TestRepository.getDeltakerSistBesokt(deltaker.id)!!
        lagretSistBesokt shouldBeCloseTo sistBesokt
    }

    @Test
    fun `upsert - gammel status - skal ikke overskrive nyere status`() {
        val gammelStatus = TestData.lagDeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            gyldigFra = LocalDateTime.now().minusMonths(3),
        )

        val deltaker = TestData.lagDeltaker(status = gammelStatus)

        TestRepository.insert(deltaker)

        val nyStatus = TestData.lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            gyldigFra = LocalDateTime.now().minusMonths(1),
        )

        repository.upsert(deltaker.copy(status = nyStatus))

        repository
            .get(deltaker.id)
            .getOrThrow()
            .status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET

        repository.upsert(deltaker.copy(status = gammelStatus))

        val statuser = repository.getDeltakerStatuser(deltaker.id)
        statuser.size shouldBe 2
        statuser.first { it.type == DeltakerStatus.Type.HAR_SLUTTET }.gyldigTil shouldBe null
        statuser.first { it.type == DeltakerStatus.Type.DELTAR }.gyldigTil shouldNotBe null
    }

    @Test
    fun `deaktiverUkritiskTidligereStatuserQuery - skal deaktivere alle andre statuser`() {
        val gammelStatus1 = TestData.lagDeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            gyldigFra = LocalDate.of(2024, 7, 14).atStartOfDay(),
            gyldigTil = LocalDate.of(2024, 10, 9).atStartOfDay(),
        )
        val gammelStatus2 = TestData.lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = DeltakerStatus.Aarsak.Type.ANNET,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val nyStatus = TestData.lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = null,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val deltaker = TestData.lagDeltaker(status = nyStatus)
        TestRepository.insert(deltaker)

        TestRepository.insert(gammelStatus1, deltaker.id)
        TestRepository.insert(gammelStatus2, deltaker.id)

        repository.deaktiverUkritiskTidligereStatuserQuery(nyStatus, deltaker.id)

        repository
            .get(deltaker.id)
            .getOrThrow()
            .status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET

        val statuser = repository.getDeltakerStatuser(deltaker.id)
        statuser.size shouldBe 3
        statuser.filter { it.gyldigTil == null }.size shouldBe 1
        statuser.first { it.gyldigTil == null }.id shouldBe nyStatus.id
    }

    @Test
    fun `getForDeltakerliste - flere deltakere på samme liste - returnerer liste med deltakere`() {
        val deltakerliste = TestData.lagDeltakerliste()
        val deltakere = (0..10).map {
            TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            )
        }
        deltakere.forEach { TestRepository.insert(it) }
        deltakere.forEach {
            TestRepository.insert(
                TestData.lagDeltakerStatus(
                    type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                    gyldigFra = it.status.gyldigFra.minusMonths(1),
                    gyldigTil = it.status.gyldigFra,
                ),
                it.id,
            )
        }

        val deltakereFraDb = repository.getForDeltakerliste(deltakerliste.id)

        deltakereFraDb.size shouldBe deltakere.size
        deltakere
            .sortedBy { it.id }
            .zip(deltakereFraDb.sortedBy { it.id })
            .forEach { sammenlignDeltakere(it.first, it.second) }
    }

    @Test
    fun `getForDeltakerliste - deltakerliste finnes ikke - returnerer tom liste`() {
        val deltakereFraDb = repository.getForDeltakerliste(UUID.randomUUID())

        deltakereFraDb shouldBe emptyList()
    }

    @Test
    fun `updateBatch - flere deltakere - oppdaterer deltakere og statuser riktig`() {
        val deltaker1 = TestData.lagDeltaker()
        val deltaker2 = TestData.lagDeltaker()

        TestRepository.insert(deltaker1)
        TestRepository.insert(deltaker2)

        val oppdatertDeltaker1 = deltaker1.copy(
            sluttdato = LocalDate.now().plusWeeks(2),
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            erManueltDeltMedArrangor = true,
        )
        val oppdatertDeltaker2 = deltaker2.copy(
            sluttdato = LocalDate.now().plusWeeks(2),
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            erManueltDeltMedArrangor = true,
        )

        repository.updateBatch(listOf(oppdatertDeltaker1, oppdatertDeltaker2))

        val deltaker1FraDB = repository.get(deltaker1.id).getOrThrow()
        sammenlignDeltakere(deltaker1FraDB, oppdatertDeltaker1)

        val deltaker2FraDB = repository.get(deltaker2.id).getOrThrow()
        sammenlignDeltakere(deltaker2FraDB, oppdatertDeltaker2)

        repository.getDeltakereMedFlereGyldigeStatuser() shouldBe emptyList()
    }

    @Test
    fun `get(list) - henter flere deltakere`() {
        val deltaker1 = TestData.lagDeltaker()
        val deltaker2 = TestData.lagDeltaker()

        TestRepository.insert(deltaker1)
        TestRepository.insert(deltaker2)

        repository.getMany(listOf(deltaker1.id, deltaker2.id)) shouldHaveSize 2
    }
}

private fun Deltaker.toDeltakeroppdatering() = Deltakeroppdatering(
    id,
    startdato,
    sluttdato,
    dagerPerUke,
    deltakelsesprosent,
    bakgrunnsinformasjon,
    deltakelsesinnhold,
    status,
    historikk,
    sistEndret,
)

fun sammenlignDeltakere(a: Deltaker, b: Deltaker) {
    a.id shouldBe b.id
    a.navBruker shouldBe b.navBruker
    a.startdato shouldBe b.startdato
    a.sluttdato shouldBe b.sluttdato
    a.dagerPerUke shouldBe b.dagerPerUke
    a.deltakelsesprosent shouldBe b.deltakelsesprosent
    a.bakgrunnsinformasjon shouldBe b.bakgrunnsinformasjon
    a.deltakelsesinnhold shouldBe b.deltakelsesinnhold
    a.historikk shouldBe b.historikk
    a.sistEndret shouldBeCloseTo b.sistEndret
    a.status.id shouldBe b.status.id
    a.status.type shouldBe b.status.type
    a.status.aarsak shouldBe b.status.aarsak
    a.status.gyldigFra shouldBeCloseTo b.status.gyldigFra
    a.status.gyldigTil shouldBeCloseTo b.status.gyldigTil
    a.status.opprettet shouldBeCloseTo b.status.opprettet
    a.erManueltDeltMedArrangor shouldBe b.erManueltDeltMedArrangor
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
    deltakelsesinnhold = deltakelsesinnhold!!,
    status = status,
)

private fun Deltaker.getAlleVedtak() = historikk.filterIsInstance<DeltakerHistorikk.Vedtak>().map { it.vedtak }
