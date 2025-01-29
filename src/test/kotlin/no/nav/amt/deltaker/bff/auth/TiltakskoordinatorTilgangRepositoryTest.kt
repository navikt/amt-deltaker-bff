package no.nav.amt.deltaker.bff.auth

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

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
    val deltakerliste: Deltakerliste = TestData.lagDeltakerliste(),
    var tilgang: TiltakskoordinatorDeltakerlisteTilgang = TestData.lagTiltakskoordinatorTilgang(
        deltakerliste = deltakerliste,
        navAnsatt = navAnsatt,
    ),
) {
    init {
        TestRepository.insert(navAnsatt)
        TestRepository.insert(deltakerliste)
    }

    fun medAktivTilgang() {
        TestRepository.insert(tilgang)
    }

    fun medInaktivTilgang() {
        tilgang = tilgang.copy(gyldigTil = LocalDateTime.now())
        TestRepository.insert(tilgang)
    }
}
