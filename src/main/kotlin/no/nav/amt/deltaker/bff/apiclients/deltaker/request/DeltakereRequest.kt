package no.nav.amt.deltaker.bff.apiclients.deltaker.request

import java.util.UUID

data class DeltakereRequest(
    val deltakere: List<UUID>,
    val endretAv: String,
)
