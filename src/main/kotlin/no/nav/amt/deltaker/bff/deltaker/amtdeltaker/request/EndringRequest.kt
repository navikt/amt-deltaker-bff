package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

sealed interface EndringRequest {
    val endretAv: String
    val endretAvEnhet: String
}

data class BakgrunnsinformasjonRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val bakgrunnsinformasjon: String?,
) : EndringRequest
