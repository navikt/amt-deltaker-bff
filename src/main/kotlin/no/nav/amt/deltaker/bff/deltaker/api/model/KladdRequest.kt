package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBakgrunnsinformasjon
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsent
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerInnhold
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class KladdRequest(
    val innhold: List<InnholdDto>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
) {
    fun valider(deltaker: Deltaker) {
        validerBakgrunnsinformasjon(bakgrunnsinformasjon)
        validerDeltakelsesProsent(deltakelsesprosent)
        validerDagerPerUke(dagerPerUke)
        validerInnhold(innhold, deltaker.deltakerliste.tiltak.innhold)
    }
}
