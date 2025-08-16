package no.nav.amt.deltaker.bff.apiclients.deltaker.request

data class BakgrunnsinformasjonRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val bakgrunnsinformasjon: String?,
) : DeltakerEndringRequest
