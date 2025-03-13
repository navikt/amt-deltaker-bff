package no.nav.amt.deltaker.bff.tiltakskoordinator.api.response

import java.util.UUID

data class KoordinatorResponse(
    val id: UUID,
    val navn: String,
)
