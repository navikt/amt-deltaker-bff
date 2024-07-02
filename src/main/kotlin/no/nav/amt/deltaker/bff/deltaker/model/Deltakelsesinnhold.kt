package no.nav.amt.deltaker.bff.deltaker.model

data class Deltakelsesinnhold(
    val ledetekst: String,
    val innhold: List<Innhold>,
)
