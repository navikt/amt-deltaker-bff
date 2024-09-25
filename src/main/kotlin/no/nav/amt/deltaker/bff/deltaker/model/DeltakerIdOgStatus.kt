package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

data class DeltakerIdOgStatus(
    val id: UUID,
    val status: DeltakerStatus,
    val kanEndres: Boolean,
)
