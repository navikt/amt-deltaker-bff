package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import no.nav.amt.lib.models.deltaker.DeltakerStatus

data class DeltakerResponse(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val status: DeltakerStatusResponse,
) {
    data class DeltakerStatusResponse(
        val type: DeltakerStatus.Type,
        val aarsak: DeltakerStatusAarsakResponse?,
    )

    data class DeltakerStatusAarsakResponse(
        val type: DeltakerStatus.Aarsak.Type,
    )
}
