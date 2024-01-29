package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBakgrunnsinformasjon
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBegrunnelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsent
import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class PameldingUtenGodkjenningRequest(
    val mal: List<Mal>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
    val begrunnelse: Begrunnelse,
) {
    fun valider() {
        validerBakgrunnsinformasjon(bakgrunnsinformasjon)
        validerBegrunnelse(begrunnelse.beskrivelse)
        validerDeltakelsesProsent(deltakelsesprosent)
        validerDagerPerUke(dagerPerUke)
    }
}

data class Begrunnelse(
    val type: String,
    val beskrivelse: String?,
)
