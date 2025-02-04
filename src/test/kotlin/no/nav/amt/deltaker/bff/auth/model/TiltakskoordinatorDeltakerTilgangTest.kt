package no.nav.amt.deltaker.bff.auth.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangContext
import org.junit.Test

class TiltakskoordinatorDeltakerTilgangTest {
    @Test
    fun `visningsnavn - adressebeskyttet og ikke tilgang - sensurerer navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medFortroligDeltaker()
        val tilgang = TiltakskoordinatorDeltakerTilgang(deltaker, false)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe "Adressebeskyttet"
        mellomnavn shouldBe null
        etternavn shouldBe ""
    }

    @Test
    fun `visningsnavn - adressebeskyttet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        medFortroligDeltaker()
        val tilgang = TiltakskoordinatorDeltakerTilgang(deltaker, true)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }

    @Test
    fun `visningsnavn - ikke adressebeskyttet og tilgang - sensurerer ikke navn`(): Unit = with(TiltakskoordinatorTilgangContext()) {
        val tilgang = TiltakskoordinatorDeltakerTilgang(deltaker, true)

        val (fornavn, mellomnavn, etternavn) = tilgang.visningsnavn()

        fornavn shouldBe deltaker.navBruker.fornavn
        mellomnavn shouldBe deltaker.navBruker.mellomnavn
        etternavn shouldBe deltaker.navBruker.etternavn
    }
}
