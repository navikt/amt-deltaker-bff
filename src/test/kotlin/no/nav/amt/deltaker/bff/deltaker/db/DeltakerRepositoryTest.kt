package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.deltaker.bff.apiclients.DtoMappers.opprettKladdResponseFromDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.utils.DeltakerTestUtils.sammenlignDeltakere
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerEndring
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerKladd
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerStatus
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.leggTilHistorikk
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.data.endre
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class DeltakerRepositoryTest {
    companion object {
        val deltakerRepository = DeltakerRepository()

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container
        }
    }

    @AfterEach
    fun cleanDatabase() = TestRepository.cleanDatabase()

    @Nested
    inner class Upsert {
        @Test
        fun `upsert - ny deltaker - insertes`() {
            val deltaker = lagDeltaker()
            TestRepository.insert(deltaker.deltakerliste)
            TestRepository.insert(deltaker.navBruker)

            deltakerRepository.upsert(deltaker)
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), deltaker)
        }

        @Test
        fun `upsert - oppdatert deltaker - oppdaterer`() {
            val deltaker = lagDeltaker()
            TestRepository.insert(deltaker)

            val oppdatertDeltaker = deltaker.copy(
                startdato = LocalDate.now().plusWeeks(1),
                sluttdato = LocalDate.now().plusWeeks(5),
                dagerPerUke = 1F,
                deltakelsesprosent = 20F,
            )

            deltakerRepository.upsert(oppdatertDeltaker)
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
        }

        @Test
        fun `upsert - ny status - inserter ny status og deaktiverer gammel`() {
            val deltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            )
            TestRepository.insert(deltaker)

            val oppdatertDeltaker = deltaker.copy(
                status = lagDeltakerStatus(
                    type = DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
                ),
            )

            deltakerRepository.upsert(oppdatertDeltaker)
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)

            val statuser = deltakerRepository.getDeltakerStatuser(deltaker.id)
            statuser.first { it.id == deltaker.status.id }.gyldigTil shouldNotBe null
            statuser.first { it.id == oppdatertDeltaker.status.id }.gyldigTil shouldBe null
        }
    }

    @Test
    fun `delete - ingen endring eller vedtak - sletter deltaker`() {
        val deltaker = lagDeltaker(status = lagDeltakerStatus(type = DeltakerStatus.Type.KLADD))
        TestRepository.insert(deltaker)

        deltakerRepository.delete(deltaker.id)

        deltakerRepository.get(deltaker.id).isFailure shouldBe true
    }

    @Test
    fun `get - deltaker har flere vedtak, et aktivt - returnerer deltaker med aktivt vedtak`() {
        val baseDeltaker = lagDeltaker()
        val deltakerInTest = leggTilHistorikk(baseDeltaker, 2)

        TestRepository.insert(deltakerInTest)

        val deltakerInDb = deltakerRepository.get(baseDeltaker.id).getOrNull()

        assertSoftly(deltakerInDb.shouldNotBeNull().fattetVedtak.shouldNotBeNull()) {
            id shouldBe deltakerInTest.getAlleVedtak().first { it.gyldigTil == null }.id
            fattet shouldNotBe null
            fattetAvNav shouldBe true
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `create - ny kladd - oppretter ny deltaker`() {
            val deltaker = lagDeltakerKladd()
            TestRepository.insert(deltaker.navBruker)
            TestRepository.insert(deltaker.deltakerliste)

            val kladd = opprettKladdResponseFromDeltaker(deltaker)

            deltakerRepository.opprettKladd(kladd)

            sammenlignDeltakere(deltaker, deltakerRepository.get(kladd.id).getOrThrow())
        }

        @Test
        fun `create - deltaker eksisterer - feiler`() {
            val deltaker = lagDeltakerKladd()
            TestRepository.insert(deltaker)

            val kladd = opprettKladdResponseFromDeltaker(deltaker)

            assertThrows(PSQLException::class.java) {
                deltakerRepository.opprettKladd(kladd)
            }
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update - deltaker er endret - oppdaterer`() {
            val sistEndret = LocalDateTime.now().minusDays(3)
            val deltaker = lagDeltaker(sistEndret = sistEndret)
            TestRepository.insert(deltaker)
            val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("ny bakgrunn for innsøk")
            val oppdatertDeltaker = deltaker.endre(lagDeltakerEndring(endring = endring))

            deltakerRepository.update(oppdatertDeltaker.toDeltakeroppdatering())
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
        }

        @Test
        fun `update - deltaker endringshistorikk mangler men er utkast - oppdaterer`() {
            val deltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            )
            TestRepository.insert(deltaker)
            val oppdatertDeltaker = deltaker.copy(bakgrunnsinformasjon = "Endringshistorikk mangler")

            deltakerRepository.update(oppdatertDeltaker.toDeltakeroppdatering())
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
        }

        @Test
        fun `update - deltakerstatus er endret - oppdaterer`() {
            val deltaker = lagDeltakerKladd()
            TestRepository.insert(deltaker)
            val oppdatertDeltaker = deltaker.copy(
                status = lagDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            )
            deltakerRepository.update(oppdatertDeltaker.toDeltakeroppdatering())
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
        }

        @Test
        fun `update - deltaker kan ikke endres - oppdaterer deltaker men beholder lasing`() {
            val sistEndret = LocalDateTime.now().minusDays(3)
            val deltaker = lagDeltaker(sistEndret = sistEndret, kanEndres = false)
            TestRepository.insert(deltaker)
            val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("ny bakgrunn for innsøk")
            val oppdatertDeltaker = deltaker.endre(lagDeltakerEndring(endring = endring))

            deltakerRepository.update(oppdatertDeltaker.toDeltakeroppdatering())
            val deltakerResultat = deltakerRepository.get(deltaker.id).getOrThrow()
            sammenlignDeltakere(deltakerResultat, oppdatertDeltaker)
            deltakerResultat.kanEndres shouldBe false
        }

        @Test
        fun `update - deltaker kan ikke endres, kun oppdatert historikk - oppdaterer historikk`() {
            val sistEndret = LocalDateTime.now().minusDays(3)
            val deltaker = lagDeltaker(sistEndret = sistEndret, kanEndres = false)
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

            deltakerRepository.update(oppdatertDeltaker.toDeltakeroppdatering())
            sammenlignDeltakere(deltakerRepository.get(deltaker.id).getOrThrow(), oppdatertDeltaker)
        }
    }

    @Nested
    inner class GetTidligereAvsluttedeDeltakelser {
        @Test
        fun `getTidligereAvsluttedeDeltakelser - har ingen tidligere deltakelse - returnerer tom liste`() {
            val deltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            )
            TestRepository.insert(deltaker)

            deltakerRepository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe emptyList()
        }

        @Test
        fun `getTidligereAvsluttedeDeltakelser - har aktiv tidligere deltakelse - returnerer tom liste`() {
            val avsluttetDeltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            )
            TestRepository.insert(avsluttetDeltaker)
            val deltaker = lagDeltaker(
                deltakerliste = avsluttetDeltaker.deltakerliste,
                navBruker = avsluttetDeltaker.navBruker,
                status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            )
            TestRepository.insert(deltaker)

            deltakerRepository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe emptyList()
        }

        @Test
        fun `getTidligereAvsluttedeDeltakelser - har tidligere avsluttet deltakelse - returnerer id`() {
            val avsluttetDeltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            )
            TestRepository.insert(avsluttetDeltaker)
            val deltaker = lagDeltaker(
                deltakerliste = avsluttetDeltaker.deltakerliste,
                navBruker = avsluttetDeltaker.navBruker,
                status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            )
            TestRepository.insert(deltaker)

            deltakerRepository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe listOf(avsluttetDeltaker.id)
        }

        @Test
        fun `getTidligereAvsluttedeDeltakelser - har tidligere avsluttet deltakelse, er avsluttet deltakelse - returnerer id`() {
            val avsluttetDeltaker = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            )
            TestRepository.insert(avsluttetDeltaker)
            val deltaker = lagDeltaker(
                deltakerliste = avsluttetDeltaker.deltakerliste,
                navBruker = avsluttetDeltaker.navBruker,
                status = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            )
            TestRepository.insert(deltaker)

            deltakerRepository.getTidligereAvsluttedeDeltakelser(deltaker.id) shouldBe listOf(avsluttetDeltaker.id)
        }
    }

    @Test
    fun `getUtdaterteKladder - finnes en utdatert kladd - returnerer utdatert kladd`() {
        val kladd = lagDeltakerKladd(sistEndret = LocalDateTime.now().minusDays(2))
        TestRepository.insert(kladd)
        val utdatertKladd = lagDeltakerKladd(sistEndret = LocalDateTime.now().minusDays(20))
        TestRepository.insert(utdatertKladd)

        val utdaterteKladder = deltakerRepository.getUtdaterteKladder(LocalDateTime.now().minusWeeks(2))

        utdaterteKladder.size shouldBe 1
        utdaterteKladder.first().id shouldBe utdatertKladd.id
    }

    @Test
    fun `oppdaterSistBesokt - oppdaterer sistBesokt - skal ikke feile`() {
        val deltaker = lagDeltaker()
        TestRepository.insert(deltaker)

        val sistBesokt = ZonedDateTime.now()

        deltakerRepository.oppdaterSistBesokt(deltaker.id, sistBesokt)

        val lagretSistBesokt = TestRepository.getDeltakerSistBesokt(deltaker.id)
        lagretSistBesokt.shouldNotBeNull()
        lagretSistBesokt shouldBeCloseTo sistBesokt
    }

    @Test
    fun `deaktiverUkritiskTidligereStatuserQuery - skal deaktivere alle andre statuser`() {
        val gammelStatus1 = lagDeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            gyldigFra = LocalDate.of(2024, 7, 14).atStartOfDay(),
            gyldigTil = LocalDate.of(2024, 10, 9).atStartOfDay(),
        )
        val gammelStatus2 = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = DeltakerStatus.Aarsak.Type.ANNET,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val nyStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = null,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val deltaker = lagDeltaker(status = nyStatus)
        TestRepository.insert(deltaker)

        TestRepository.insert(gammelStatus1, deltaker.id)
        TestRepository.insert(gammelStatus2, deltaker.id)

        deltakerRepository.deaktiverUkritiskTidligereStatuserQuery(nyStatus, deltaker.id)

        deltakerRepository
            .get(deltaker.id)
            .getOrThrow()
            .status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET

        val statuser = deltakerRepository.getDeltakerStatuser(deltaker.id)
        statuser.size shouldBe 3
        statuser.filter { it.gyldigTil == null }.size shouldBe 1
        statuser.first { it.gyldigTil == null }.id shouldBe nyStatus.id
    }

    @Nested
    inner class GetForDeltakerliste {
        @Test
        fun `getForDeltakerliste - flere deltakere pa samme liste - returnerer liste med deltakere`() {
            val deltakerliste = lagDeltakerliste()
            val deltakere = (0..10).map {
                lagDeltaker(
                    deltakerliste = deltakerliste,
                    status = lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
                )
            }
            deltakere.forEach { TestRepository.insert(it) }
            deltakere.forEach {
                TestRepository.insert(
                    lagDeltakerStatus(
                        type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                        gyldigFra = it.status.gyldigFra.minusMonths(1),
                        gyldigTil = it.status.gyldigFra,
                    ),
                    it.id,
                )
            }

            val deltakereFraDb = deltakerRepository.getForDeltakerliste(deltakerliste.id)

            deltakereFraDb.size shouldBe deltakere.size
            deltakere.sortedBy { it.id }.zip(deltakereFraDb.sortedBy { it.id }).forEach { sammenlignDeltakere(it.first, it.second) }
        }

        @Test
        fun `getForDeltakerliste - deltakerliste finnes ikke - returnerer tom liste`() {
            val deltakereFraDb = deltakerRepository.getForDeltakerliste(UUID.randomUUID())

            deltakereFraDb shouldBe emptyList()
        }
    }

    @Test
    fun `updateBatch - flere deltakere - oppdaterer deltakere og statuser riktig`() {
        val deltaker1 = lagDeltaker()
        val deltaker2 = lagDeltaker()

        TestRepository.insert(deltaker1)
        TestRepository.insert(deltaker2)

        val oppdatertDeltaker1 = deltaker1.copy(
            sluttdato = LocalDate.now().plusWeeks(2),
            status = lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            erManueltDeltMedArrangor = true,
        )

        val oppdatertDeltaker2 = deltaker2.copy(
            sluttdato = LocalDate.now().plusWeeks(2),
            status = lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            erManueltDeltMedArrangor = true,
        )

        deltakerRepository.updateBatch(
            listOf(
                oppdatertDeltaker1.toDeltakeroppdatering(),
                oppdatertDeltaker2.toDeltakeroppdatering(),
            ),
        )

        val deltaker1FraDB = deltakerRepository.get(deltaker1.id).getOrThrow()
        sammenlignDeltakere(deltaker1FraDB, oppdatertDeltaker1)

        val deltaker2FraDB = deltakerRepository.get(deltaker2.id).getOrThrow()
        sammenlignDeltakere(deltaker2FraDB, oppdatertDeltaker2)

        deltakerRepository.getDeltakereMedFlereGyldigeStatuser() shouldBe emptyList()
    }

    @Test
    fun `getDeltakereMedFlereGyldigeStatuser - deltaker har flere statuser som er gyldig - returnerer statuser`() {
        val deltakerInTest = lagDeltaker(
            status = lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltakerInTest)

        val oppdatertStatus: DeltakerStatus = lagDeltakerStatus(DeltakerStatus.Type.FULLFORT)
        TestRepository.insert(oppdatertStatus, deltakerInTest.id)

        deltakerRepository.getDeltakereMedFlereGyldigeStatuser().shouldNotBeEmpty()
    }

    @Nested
    inner class GetManyByIdList {
        @Test
        fun `getMany - ingen deltakere - returnerer tom liste`() {
            deltakerRepository.getMany(emptyList()).shouldBeEmpty()
        }

        @Test
        fun `getMany - henter flere deltakere`() {
            val deltaker1 = lagDeltaker()
            val deltaker2 = lagDeltaker()

            TestRepository.insert(deltaker1)
            TestRepository.insert(deltaker2)

            deltakerRepository.getMany(listOf(deltaker1.id, deltaker2.id)) shouldHaveSize 2
        }
    }

    @Nested
    inner class GetManyByPersonIdentAndDeltakerlisteId {
        @Test
        fun `getMany - ingen deltakere - returnerer tom liste`() {
            deltakerRepository.getMany("~personident~", UUID.randomUUID()).shouldBeEmpty()
        }

        @Test
        fun `getMany - henter flere deltakere`() {
            val arrangor = TestData.lagArrangor()
            TestRepository.insert(arrangor)

            val deltakerliste = lagDeltakerliste(arrangor = arrangor)

            val deltakerInTest = lagDeltaker(deltakerliste = deltakerliste)
            TestRepository.insert(deltakerInTest)

            deltakerRepository
                .getMany(
                    personident = deltakerInTest.navBruker.personident,
                    deltakerlisteId = deltakerliste.id,
                ).shouldNotBeEmpty()
        }
    }

    @Nested
    inner class GetManyByPersonIdent {
        @Test
        fun `getMany - ingen deltakere - returnerer tom liste`() {
            deltakerRepository.getMany("~personident~").shouldBeEmpty()
        }

        @Test
        fun `getMany - henter flere deltakere`() {
            val deltakerInTest = lagDeltaker()
            TestRepository.insert(deltakerInTest)

            deltakerRepository
                .getMany(personident = deltakerInTest.navBruker.personident)
                .shouldNotBeEmpty()
        }
    }

    @Nested
    inner class GetKladderForDeltakerlisteByDeltakerListeId {
        @Test
        fun `getKladderForDeltakerliste - ingen deltakere - returnerer tom liste`() {
            deltakerRepository
                .getKladderForDeltakerliste(UUID.randomUUID())
                .shouldBeEmpty()
        }

        @Test
        fun `getKladderForDeltakerliste - henter flere deltakere`() {
            val arrangor = TestData.lagArrangor()
            TestRepository.insert(arrangor)

            val deltakerliste = lagDeltakerliste(arrangor = arrangor)

            val deltakerInTest = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.KLADD),
                deltakerliste = deltakerliste,
            )
            TestRepository.insert(deltakerInTest)

            deltakerRepository
                .getKladderForDeltakerliste(deltakerliste.id)
                .shouldNotBeEmpty()
        }
    }

    @Nested
    inner class GetKladdForDeltakerliste {
        @Test
        fun `getKladdForDeltakerliste - ingen deltakere - returnerer null`() {
            deltakerRepository
                .getKladdForDeltakerliste(
                    deltakerlisteId = UUID.randomUUID(),
                    personident = "~personindent~",
                ).shouldBeNull()
        }

        @Test
        fun `getKladdForDeltakerliste - henter deltaker`() {
            val arrangor = TestData.lagArrangor()
            TestRepository.insert(arrangor)

            val deltakerliste = lagDeltakerliste(arrangor = arrangor)

            val deltakerInTest = lagDeltaker(
                status = lagDeltakerStatus(type = DeltakerStatus.Type.KLADD),
                deltakerliste = deltakerliste,
            )
            TestRepository.insert(deltakerInTest)

            deltakerRepository
                .getKladdForDeltakerliste(
                    deltakerlisteId = deltakerliste.id,
                    personident = deltakerInTest.navBruker.personident,
                ).shouldNotBeNull()
        }
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
    erManueltDeltMedArrangor,
)

private fun Deltaker.getAlleVedtak() = historikk.filterIsInstance<DeltakerHistorikk.Vedtak>().map { it.vedtak }
