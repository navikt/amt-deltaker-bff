package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

data class DeltakerResponse(
    val id: UUID,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val status: DeltakerStatusResponse,
    val beskyttelsesmarkering: List<Beskyttelsesmarkering>,
    val vurdering: Vurderingstype?,
) {
    data class DeltakerStatusResponse(
        val type: DeltakerStatus.Type,
        val aarsak: DeltakerStatusAarsakResponse?,
    )

    data class DeltakerStatusAarsakResponse(
        val type: DeltakerStatus.Aarsak.Type,
    )

    enum class Beskyttelsesmarkering {
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND,
        SKJERMET,
    }
}
