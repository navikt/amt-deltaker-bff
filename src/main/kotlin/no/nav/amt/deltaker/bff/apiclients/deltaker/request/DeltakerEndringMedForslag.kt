package no.nav.amt.deltaker.bff.apiclients.deltaker.request

import java.util.UUID

sealed interface DeltakerEndringMedForslag : DeltakerEndringRequest {
    val forslagId: UUID?
}
