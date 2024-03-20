package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesinnhold
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class EndreInnholdRequest(
    val innhold: List<InnholdDto>,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerDeltakelsesinnhold(innhold, opprinneligDeltaker.deltakerliste.tiltak.innhold)
        validerDeltakerKanEndres(opprinneligDeltaker)
    }
}
