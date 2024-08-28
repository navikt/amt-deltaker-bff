package no.nav.amt.deltaker.bff.deltaker.model

import java.util.UUID

data class Pamelding(
    val deltakelsesinnhold: Deltakelsesinnhold,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val endretAv: String,
    val endretAvEnhet: String,
)

data class Kladd(
    val opprinneligDeltaker: Deltaker,
    val pamelding: Pamelding,
)

data class Utkast(
    val deltakerId: UUID,
    val pamelding: Pamelding,
    val godkjentAvNav: Boolean,
)
