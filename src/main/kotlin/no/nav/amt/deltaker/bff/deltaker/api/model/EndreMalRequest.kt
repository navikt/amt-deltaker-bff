package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class EndreMalRequest(
    val mal: List<Mal>,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke endre mål for deltaker som har sluttet"
        }
    }
}
