package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus

data class EndreSluttarsakRequest(
    val aarsak: DeltakerEndring.Aarsak,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(opprinneligDeltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET) {
            "Kan ikke endre sluttårsak for deltaker som ikke har sluttet"
        }
    }
}
