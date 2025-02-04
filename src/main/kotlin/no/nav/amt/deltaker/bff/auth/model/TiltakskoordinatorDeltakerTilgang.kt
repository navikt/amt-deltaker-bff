package no.nav.amt.deltaker.bff.auth.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class TiltakskoordinatorDeltakerTilgang(
    val deltaker: Deltaker,
    val tilgang: Boolean,
) {
    fun visningsnavn(): Triple<String, String?, String> = with(deltaker) {
        if (navBruker.erAdressebeskyttet && !tilgang) {
            return Triple("Adressebeskyttet", null, "")
        }

        return Triple(navBruker.fornavn, navBruker.mellomnavn, navBruker.etternavn)
    }
}
