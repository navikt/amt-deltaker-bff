package no.nav.amt.deltaker.bff.tiltakskoordinator.api.response

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.DeltakerOppdateringFeilkode
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Beskyttelsesmarkering
import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import java.util.UUID

data class DeltakerResponse(
    val id: UUID,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val status: DeltakerStatusResponse,
    val beskyttelsesmarkering: List<Beskyttelsesmarkering>,
    val vurdering: Vurderingstype?,
    val navEnhet: String?,
    val erManueltDeltMedArrangor: Boolean,
    val feilkode: DeltakerOppdateringFeilkode? = null,
)
