package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class ForslagRequest(
    val mal: List<Mal>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
)
