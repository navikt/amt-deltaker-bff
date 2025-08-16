package no.nav.amt.deltaker.bff.apiclients.deltaker.request

data class ReaktiverDeltakelseRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val begrunnelse: String,
) : DeltakerEndringRequest
