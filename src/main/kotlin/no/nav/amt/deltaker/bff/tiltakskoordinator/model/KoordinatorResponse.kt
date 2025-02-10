package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import java.util.UUID

data class KoordinatorResponse(
    val id: UUID,
    val navn: String,
)
