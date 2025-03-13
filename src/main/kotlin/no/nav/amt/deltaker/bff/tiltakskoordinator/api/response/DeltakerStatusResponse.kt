package no.nav.amt.deltaker.bff.tiltakskoordinator.api.response
import no.nav.amt.lib.models.deltaker.DeltakerStatus

data class DeltakerStatusResponse(
    val type: DeltakerStatus.Type,
    val aarsak: DeltakerStatusAarsakResponse?,
)

data class DeltakerStatusAarsakResponse(
    val type: DeltakerStatus.Aarsak.Type,
    val beskrivelse: String?,
)

fun DeltakerStatus.toResponse() = DeltakerStatusResponse(
    type = type,
    aarsak = aarsak?.let {
        DeltakerStatusAarsakResponse(
            it.type,
            it.beskrivelse,
        )
    },
)
