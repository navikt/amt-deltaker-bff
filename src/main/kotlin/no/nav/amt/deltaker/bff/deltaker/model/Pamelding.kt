package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class Pamelding(
    val mal: List<Mal>,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val endretAv: String,
    val endretAvEnhet: String?,
)

data class Kladd(
    val opprinneligDeltaker: Deltaker,
    val pamelding: Pamelding,
)

data class Utkast(
    val opprinneligDeltaker: Deltaker,
    val pamelding: Pamelding,
    val godkjentAvNav: GodkjenningAvNav?,
)
