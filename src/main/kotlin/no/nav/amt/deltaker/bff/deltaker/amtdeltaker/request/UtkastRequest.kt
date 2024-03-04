package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import no.nav.amt.deltaker.bff.deltaker.model.Innhold

data class UtkastRequest(
    val innhold: List<Innhold>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val endretAv: String,
    val endretAvEnhet: String,
    val godkjentAvNav: Boolean,
)
