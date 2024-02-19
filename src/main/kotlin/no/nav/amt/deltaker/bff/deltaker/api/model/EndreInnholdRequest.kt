package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerInnhold
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class EndreInnholdRequest(
    val innhold: List<InnholdDto>,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke endre innhold for deltaker som har sluttet"
        }
        validerInnhold(innhold, opprinneligDeltaker.deltakerliste.tiltak.innhold)
    }
}
