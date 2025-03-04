package no.nav.amt.deltaker.bff.auth

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.Adressebeskyttelse
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakskoordinatorTilgangRepositoryTest {
    @Before
    fun setup() {
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

    @Test
    fun `hentKoordinatorer - deltakerliste har ikke koordinatorer - returnerer tom liste`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            repository.hentKoordinatorer(UUID.randomUUID()) shouldBe emptyList()
        }
    }

    @Test
    fun `hentKoordinatorer - deltakerliste har koordinatorer - returnerer nav ansatte`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            val navAnsatte = repository.hentKoordinatorer(deltakerliste.id)
            navAnsatte shouldHaveSize 1
            navAnsatte.first().navn shouldBeEqual "Veileder Veiledersen"
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
}

fun sammenlignTilganger(tilgang1: TiltakskoordinatorDeltakerlisteTilgang, tilgang2: TiltakskoordinatorDeltakerlisteTilgang) {
    tilgang1.id shouldBe tilgang2.id
    tilgang1.navAnsattId shouldBe tilgang2.navAnsattId
    tilgang1.deltakerlisteId shouldBe tilgang2.deltakerlisteId
    tilgang1.gyldigFra shouldBeCloseTo tilgang2.gyldigFra
    tilgang1.gyldigTil shouldBeCloseTo tilgang2.gyldigTil
}

data class TiltakskoordinatorTilgangContext(
    val navAnsatt: NavAnsatt = TestData.lagNavAnsatt(),
    var deltakerliste: Deltakerliste = TestData.lagDeltakerliste(),
    var tilgang: TiltakskoordinatorDeltakerlisteTilgang = TestData.lagTiltakskoordinatorTilgang(
        deltakerliste = deltakerliste,
        navAnsatt = navAnsatt,
    ),
    var deltaker: Deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste),
) {
    val deltakerRepository = DeltakerRepository()
    val deltakerlisteRepository = DeltakerlisteRepository()
    val navAnsattAzureId = UUID.randomUUID()

    init {
        SingletonPostgres16Container
        TestRepository.insert(navAnsatt)
        TestRepository.insert(deltakerliste)
        TestRepository.insert(deltaker)
    }

    fun medAktivTilgang() {
        TestRepository.insert(tilgang)
    }

    fun medInaktivTilgang() {
        tilgang = tilgang.copy(gyldigTil = LocalDateTime.now())
        TestRepository.insert(tilgang)
    }

    fun medSkjermetDeltaker() {
        deltaker = deltaker.copy(navBruker = deltaker.navBruker.copy(erSkjermet = true))
        deltakerRepository.upsert(deltaker)
    }

    fun medFortroligDeltaker() = adressebeskyttetDeltaker(Adressebeskyttelse.FORTROLIG)

    fun medStrengtFortroligDeltaker() = adressebeskyttetDeltaker(Adressebeskyttelse.STRENGT_FORTROLIG)

    fun medStengtDeltakerliste() {
        deltakerliste = deltakerliste.copy(
            status = Deltakerliste.Status.AVSLUTTET,
            sluttDato = LocalDate.now().minus(DeltakerlisteService.tiltakskoordinatorGraceperiode).minusDays(1),
        )
        deltakerlisteRepository.upsert(deltakerliste)
    }

    fun medAvsluttetDeltakerliste() {
        deltakerliste = deltakerliste.copy(
            status = Deltakerliste.Status.AVSLUTTET,
            sluttDato = LocalDate.now(),
        )
        deltakerlisteRepository.upsert(deltakerliste)
    }

    private fun adressebeskyttetDeltaker(adressebeskyttelse: Adressebeskyttelse?) {
        deltaker = deltaker.copy(navBruker = deltaker.navBruker.copy(adressebeskyttelse = adressebeskyttelse))
        deltakerRepository.upsert(deltaker)
    }
}
