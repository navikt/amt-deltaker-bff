package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBegrunnelse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
    val begrunnelse: String?,
    override val forslagId: UUID?,
) : EndringsforslagRequest {
    override fun valider(deltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(deltaker.status.type == DeltakerStatus.Type.VENTER_PA_OPPSTART) {
            "Kan ikke sette deltaker med status ${deltaker.status.type} til ikke aktuell"
        }
        validerBegrunnelse(begrunnelse)
    }
}
