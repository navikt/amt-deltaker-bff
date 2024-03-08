package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import no.nav.amt.deltaker.bff.deltaker.model.Innhold

sealed interface EndringRequest {
    val endretAv: String
    val endretAvEnhet: String
}

data class BakgrunnsinformasjonRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val bakgrunnsinformasjon: String?,
) : EndringRequest

data class InnholdRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val innhold: List<Innhold>,
) : EndringRequest
