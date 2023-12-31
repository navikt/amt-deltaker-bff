package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class OppdatertDeltaker(
    val mal: List<Mal>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val godkjentAvNav: GodkjenningAvNav?,
    val endretAv: String,
    val endretAvEnhet: String?,
)
