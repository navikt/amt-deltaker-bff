package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Innhold

data class EndreInnholdRequest(
    val innhold: List<Innhold>,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke endre innhold for deltaker som har sluttet"
        }
    }
}
