package no.nav.amt.deltaker.bff.auth.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class TiltakskoordinatorDeltakerTilgang(
    val deltaker: Deltaker,
    val tilgang: Boolean,
) {
    fun visningsnavn(): Triple<String, String?, String> = with(deltaker) {
        if (navBruker.erAdressebeskyttet && !tilgang) {
            return Triple(ADRESSEBESKYTTET_PLACEHOLDER_NAVN, null, "")
        }
        if (navBruker.erSkjermet && !tilgang) {
            return Triple(SKJERMET_PERSON_PLACEHOLDER_NAVN, null, "")
        }

        return Triple(navBruker.fornavn, navBruker.mellomnavn, navBruker.etternavn)
    }

    companion object {
        const val ADRESSEBESKYTTET_PLACEHOLDER_NAVN = "Adressebeskyttet"
        const val SKJERMET_PERSON_PLACEHOLDER_NAVN = "Skjermet person"
    }
}
