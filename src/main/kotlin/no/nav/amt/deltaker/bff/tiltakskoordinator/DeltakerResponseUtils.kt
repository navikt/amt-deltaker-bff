package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.tiltakskoordinator.DeltakerResponseUtils.Companion.ADRESSEBESKYTTET_PLACEHOLDER_NAVN
import no.nav.amt.deltaker.bff.tiltakskoordinator.DeltakerResponseUtils.Companion.SKJERMET_PERSON_PLACEHOLDER_NAVN
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Beskyttelsesmarkering
import no.nav.amt.lib.models.arrangor.melding.Vurdering

class DeltakerResponseUtils(
    val deltaker: Deltaker,
    val tilgang: Boolean,
    val vurdering: Vurdering?,
) {
    fun visningsnavn(): Triple<String, String?, String> = with(deltaker) {
        return navBruker.getVisningsnavn(tilgang)
    }

    fun beskyttelsesmarkering(): List<Beskyttelsesmarkering> {
        if (!tilgang) return emptyList()
        return deltaker.navBruker.getBeskyttelsesmarkeringer()
    }

    companion object {
        const val ADRESSEBESKYTTET_PLACEHOLDER_NAVN = "Adressebeskyttet"
        const val SKJERMET_PERSON_PLACEHOLDER_NAVN = "Skjermet person"
    }
}

fun NavBruker.getVisningsnavn(tilgangTilBruker: Boolean): Triple<String, String?, String> {
    if (erAdressebeskyttet && !tilgangTilBruker) {
        return Triple(ADRESSEBESKYTTET_PLACEHOLDER_NAVN, null, "")
    }
    if (erSkjermet && !tilgangTilBruker) {
        return Triple(SKJERMET_PERSON_PLACEHOLDER_NAVN, null, "")
    }

    return Triple(fornavn, mellomnavn, etternavn)
}
