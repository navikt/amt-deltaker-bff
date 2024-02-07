package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBakgrunnsinformasjon
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsent
import no.nav.amt.deltaker.bff.deltakerliste.Innhold

data class PameldingUtenGodkjenningRequest(
    val innhold: List<Innhold>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
) {
    fun valider() {
        validerBakgrunnsinformasjon(bakgrunnsinformasjon)
        validerDeltakelsesProsent(deltakelsesprosent)
        validerDagerPerUke(dagerPerUke)
    }
}
