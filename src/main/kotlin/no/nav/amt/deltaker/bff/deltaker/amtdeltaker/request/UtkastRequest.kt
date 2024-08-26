package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import no.nav.amt.deltaker.bff.deltaker.model.Deltakelsesinnhold

data class UtkastRequest(
    val deltakelsesinnhold: Deltakelsesinnhold,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val endretAv: String,
    val endretAvEnhet: String,
    val godkjentAvNav: Boolean,
)
