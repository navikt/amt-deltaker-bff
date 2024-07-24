package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesinnhold
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class EndreInnholdRequest(
    val innhold: List<InnholdDto>,
) : Endringsrequest {
    override fun valider(deltaker: Deltaker) {
        validerDeltakelsesinnhold(innhold, deltaker.deltakerliste.tiltak.innhold)
        validerDeltakerKanEndres(deltaker)
    }
}
