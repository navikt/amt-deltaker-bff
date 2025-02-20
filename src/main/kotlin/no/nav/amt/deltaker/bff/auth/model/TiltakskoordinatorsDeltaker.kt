package no.nav.amt.deltaker.bff.auth.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.Adressebeskyttelse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.lib.models.arrangor.melding.Vurdering

data class TiltakskoordinatorsDeltaker(
    val deltaker: Deltaker,
    val tilgang: Boolean,
    val vurdering: Vurdering?,
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

    fun beskyttelsesmarkering(): List<DeltakerResponse.Beskyttelsesmarkering> {
        if (!tilgang) return emptyList()
        val adressebeskyttelse =
            when (deltaker.navBruker.adressebeskyttelse) {
                null -> null
                Adressebeskyttelse.FORTROLIG -> DeltakerResponse.Beskyttelsesmarkering.FORTROLIG
                Adressebeskyttelse.STRENGT_FORTROLIG -> DeltakerResponse.Beskyttelsesmarkering.STRENGT_FORTROLIG
                Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND -> DeltakerResponse.Beskyttelsesmarkering.STRENGT_FORTROLIG_UTLAND
            }

        val skjermet = if (deltaker.navBruker.erSkjermet) {
            DeltakerResponse.Beskyttelsesmarkering.SKJERMET
        } else {
            null
        }

        return listOfNotNull(adressebeskyttelse, skjermet)
    }

    companion object {
        const val ADRESSEBESKYTTET_PLACEHOLDER_NAVN = "Adressebeskyttet"
        const val SKJERMET_PERSON_PLACEHOLDER_NAVN = "Skjermet person"
    }
}
