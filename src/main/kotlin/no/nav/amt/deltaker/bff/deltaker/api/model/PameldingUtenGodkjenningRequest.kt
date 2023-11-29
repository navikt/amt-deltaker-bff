package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class PameldingUtenGodkjenningRequest(
    val mal: List<Mal>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val begrunnelse: Begrunnelse,
)

data class Begrunnelse(
    val type: String,
    val beskrivelse: String?,
)
