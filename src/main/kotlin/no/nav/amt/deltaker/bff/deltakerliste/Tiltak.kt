package no.nav.amt.deltaker.bff.deltakerliste

data class Tiltak(
    val navn: String,
    val type: Type,
) {
    enum class Type {
        INDOPPFAG,
        ARBFORB,
        AVKLARAG,
        VASV,
        ARBRRHDAG,
        DIGIOPPARB,
        JOBBK,
        GRUPPEAMO,
        GRUFAGYRKE,
    }
}

fun arenaKodeTilTiltakstype(type: String?) = when (type) {
    "ARBFORB" -> Tiltak.Type.ARBFORB
    "ARBRRHDAG" -> Tiltak.Type.ARBRRHDAG
    "AVKLARAG" -> Tiltak.Type.AVKLARAG
    "DIGIOPPARB" -> Tiltak.Type.DIGIOPPARB
    "GRUPPEAMO" -> Tiltak.Type.GRUPPEAMO
    "INDOPPFAG" -> Tiltak.Type.INDOPPFAG
    "JOBBK" -> Tiltak.Type.JOBBK
    "VASV" -> Tiltak.Type.VASV
    "GRUFAGYRKE" -> Tiltak.Type.GRUFAGYRKE
    else -> error("Ukjent tiltakstype: $type")
}
