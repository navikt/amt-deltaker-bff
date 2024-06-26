package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(opprinneligDeltaker.status.type == DeltakerStatus.Type.VENTER_PA_OPPSTART) {
            "Kan ikke sette deltaker med status ${opprinneligDeltaker.status.type} til ikke aktuell"
        }
    }
}
