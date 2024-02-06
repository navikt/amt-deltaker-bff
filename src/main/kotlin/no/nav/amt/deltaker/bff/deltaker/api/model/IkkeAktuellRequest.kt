package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke sette deltaker som har sluttet til IKKE AKTUELL"
        }
    }
}
