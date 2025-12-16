package no.nav.amt.deltaker.bff.auth

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakskoordinatorTilgang
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.person.NavAnsatt
import no.nav.amt.lib.models.person.address.Adressebeskyttelse
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakskoordinatorTilgangRepositoryTest {
    @BeforeEach
    fun setup() {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
    }

    private val repository = TiltakskoordinatorTilgangRepository()

    @Test
    fun `upsert - ny tilgang - inserter og returnerer tilgang`() {
        with(TiltakskoordinatorTilgangContext()) {
            val lagretTilgang = repository.upsert(tilgang).getOrThrow()
            sammenlignTilganger(lagretTilgang, tilgang)
        }
    }

    @Test
    fun `upsert - endret tilgang - oppdaterer og returnerer tilgang`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            val avsluttetTilgang = tilgang.copy(gyldigTil = LocalDateTime.now())
            val lagretTilgang = repository.upsert(avsluttetTilgang).getOrThrow()
            sammenlignTilganger(avsluttetTilgang, lagretTilgang)
        }
    }

    @Test
    fun `hentAktivTilgang - aktiv tilgang finnes ikke - returnerer failure`() {
        with(TiltakskoordinatorTilgangContext()) {
            val resultat = repository.hentAktivTilgang(navAnsatt.id, deltakerliste.id)
            resultat.isFailure shouldBe true
            resultat.exceptionOrNull()!!::class shouldBe NoSuchElementException::class
        }
    }

    @Test
    fun `hentAktivTilgang - inaktiv tilgang finnes - returnerer failure`() {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            val resultat = repository.hentAktivTilgang(navAnsatt.id, deltakerliste.id)
            resultat.isFailure shouldBe true
            resultat.exceptionOrNull()!!::class shouldBe NoSuchElementException::class
        }
    }

    @Test
    fun `hentAktivTilgang - aktiv tilgang finnes - returnerer succses`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            val resultat = repository.hentAktivTilgang(navAnsatt.id, deltakerliste.id)
            resultat.isSuccess shouldBe true
            sammenlignTilganger(resultat.getOrThrow(), tilgang)
        }
    }

    @Nested
    inner class HentKoordinatorer {
        @Test
        fun `deltakerliste har ingen koordinatorer - returnerer tom liste`() {
            with(TiltakskoordinatorTilgangContext()) {
                medAktivTilgang()
                val koordinatorer = repository.hentKoordinatorer(UUID.randomUUID(), UUID.randomUUID())
                koordinatorer.shouldBeEmpty()
            }
        }

        @Test
        fun `deltakerliste har aktiv koordinator - skal returnere aktiv koordinator`() {
            with(TiltakskoordinatorTilgangContext()) {
                medAktivTilgang()
                val koordinatorer = repository.hentKoordinatorer(deltakerliste.id, UUID.randomUUID())
                koordinatorer shouldHaveSize 1

                assertSoftly(koordinatorer.first()) {
                    navn shouldBe "Veileder Veiledersen"
                    erAktiv shouldBe true
                }
            }
        }

        @Test
        fun `deltakerliste har aktiv og inaktiv koordinator - returnerer begge koordinatorer`() {
            with(TiltakskoordinatorTilgangContext()) {
                medAktivTilgang()
                medInaktivTilgang()

                val koordinatorer = repository.hentKoordinatorer(deltakerliste.id, UUID.randomUUID())

                koordinatorer shouldHaveSize 2

                koordinatorer.count { it.erAktiv } shouldBe 1
                koordinatorer.count { !it.erAktiv } shouldBe 1
            }
        }

        @Test
        fun `deltakerliste har kun inaktiv koordinator - returnerer inaktiv koordinator`() {
            with(TiltakskoordinatorTilgangContext()) {
                medInaktivTilgang()

                val koordinatorer = repository.hentKoordinatorer(deltakerliste.id, UUID.randomUUID())

                koordinatorer shouldHaveSize 1
                koordinatorer.first().erAktiv shouldBe false
            }
        }

        @Test
        fun `samme koordinator finnes som inaktiv og aktiv - returnerer aktiv koordinator`() {
            with(TiltakskoordinatorTilgangContext()) {
                medAktivTilgang()
                TestRepository.insert(
                    secondTilgang.copy(
                        navAnsattId = navAnsatt.id,
                        gyldigTil = LocalDateTime.now(),
                    ),
                )

                val koordinatorer = repository.hentKoordinatorer(deltakerliste.id, UUID.randomUUID())

                koordinatorer shouldHaveSize 1
                koordinatorer.first().erAktiv shouldBe true
            }
        }
    }

    @Test
    fun `hentUtdaterteTilganger - deltakerlisten er avsluttet og stengt - returnerer utdatert tilgang`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            medStengtDeltakerliste()
            repository.hentUtdaterteTilganger() shouldHaveSize 1
        }
    }

    @Test
    fun `hentUtdaterteTilganger - deltakerlisten er avsluttet men ikke stengt - returnerer ikke utdatert tilgang`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            medAvsluttetDeltakerliste()
            repository.hentUtdaterteTilganger() shouldHaveSize 0
        }
    }

    @Test
    fun `hentAktiveForDeltakerliste - aktiv tilgang - henter tilganger pa deltakerliste`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            repository.hentAktiveForDeltakerliste(deltakerliste.id) shouldHaveSize 1
        }
    }

    @Test
    fun `hentAktiveForDeltakerliste - inaktiv tilgang - henter ikke tilganger pa deltakerliste`() {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            repository.hentAktiveForDeltakerliste(deltakerliste.id) shouldHaveSize 0
        }
    }
}

fun sammenlignTilganger(tilgang1: TiltakskoordinatorDeltakerlisteTilgang, tilgang2: TiltakskoordinatorDeltakerlisteTilgang) {
    tilgang1.id shouldBe tilgang2.id
    tilgang1.navAnsattId shouldBe tilgang2.navAnsattId
    tilgang1.deltakerlisteId shouldBe tilgang2.deltakerlisteId
    tilgang1.gyldigFra shouldBeCloseTo tilgang2.gyldigFra
    tilgang1.gyldigTil shouldBeCloseTo tilgang2.gyldigTil
}

data class TiltakskoordinatorTilgangContext(
    val navAnsatt: NavAnsatt = lagNavAnsatt(),
    val secondNavAnsatt: NavAnsatt = lagNavAnsatt(navn = "Nav Navesen"),
    var deltakerliste: Deltakerliste = lagDeltakerliste(),
    var tilgang: TiltakskoordinatorDeltakerlisteTilgang = lagTiltakskoordinatorTilgang(
        deltakerliste = deltakerliste,
        navAnsatt = navAnsatt,
    ),
    var secondTilgang: TiltakskoordinatorDeltakerlisteTilgang = lagTiltakskoordinatorTilgang(
        deltakerliste = deltakerliste,
        navAnsatt = secondNavAnsatt,
    ),
    var deltaker: Deltaker = lagDeltaker(deltakerliste = deltakerliste),
) {
    val deltakerRepository = DeltakerRepository()
    val deltakerlisteRepository = DeltakerlisteRepository()
    val navAnsattAzureId: UUID = UUID.randomUUID()

    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container

        TestRepository.insert(navAnsatt)
        TestRepository.insert(secondNavAnsatt)
        TestRepository.insert(deltakerliste)
        TestRepository.insert(deltaker)
    }

    fun medAktivTilgang() {
        TestRepository.insert(tilgang)
    }

    fun medInaktivTilgang() {
        TestRepository.insert(secondTilgang.copy(gyldigTil = LocalDateTime.now()))
    }

    fun medSkjermetDeltaker() {
        deltaker = deltaker.copy(navBruker = deltaker.navBruker.copy(erSkjermet = true))
        deltakerRepository.upsert(deltaker)
    }

    fun medFortroligDeltaker() = adressebeskyttetDeltaker(Adressebeskyttelse.FORTROLIG)

    fun medStrengtFortroligDeltaker() = adressebeskyttetDeltaker(Adressebeskyttelse.STRENGT_FORTROLIG)

    fun medStengtDeltakerliste() {
        deltakerliste = deltakerliste.copy(
            status = GjennomforingStatusType.AVSLUTTET,
            sluttDato = LocalDate.now().minus(DeltakerlisteService.tiltakskoordinatorGraceperiode).minusDays(1),
        )
        deltakerlisteRepository.upsert(deltakerliste)
    }

    fun medAvsluttetDeltakerliste() {
        deltakerliste = deltakerliste.copy(
            status = GjennomforingStatusType.AVSLUTTET,
            sluttDato = LocalDate.now(),
        )
        deltakerlisteRepository.upsert(deltakerliste)
    }

    private fun adressebeskyttetDeltaker(adressebeskyttelse: Adressebeskyttelse?) {
        deltaker = deltaker.copy(navBruker = deltaker.navBruker.copy(adressebeskyttelse = adressebeskyttelse))
        deltakerRepository.upsert(deltaker)
    }
}
