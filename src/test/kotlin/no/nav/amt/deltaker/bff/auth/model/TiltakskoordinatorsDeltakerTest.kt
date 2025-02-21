package no.nav.amt.deltaker.bff.auth.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangContext
import org.junit.Test

class TiltakskoordinatorsDeltakerTest {
    @Test
    fun `visningsnavn - adressebeskyttet og ikke tilgang - sensurerer navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medFortroligDeltaker()
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, false, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe TiltakskoordinatorsDeltaker.ADRESSEBESKYTTET_PLACEHOLDER_NAVN
        mellomnavn shouldBe null
        etternavn shouldBe ""
    }

    @Test
    fun `visningsnavn - adressebeskyttet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medFortroligDeltaker()
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, true, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }

    @Test
    fun `visningsnavn - ikke adressebeskyttet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, true, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }

    @Test
    fun `visningsnavn - skjermet og ikke tilgang - sensurerer navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medSkjermetDeltaker()
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, false, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe TiltakskoordinatorsDeltaker.SKJERMET_PERSON_PLACEHOLDER_NAVN
        mellomnavn shouldBe null
        etternavn shouldBe ""
    }

    @Test
    fun `visningsnavn - skjermet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medSkjermetDeltaker()
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, true, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }

    @Test
    fun `visningsnavn - ikke skjermet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        val tilgang = TiltakskoordinatorsDeltaker(deltaker, true, null)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }
}
