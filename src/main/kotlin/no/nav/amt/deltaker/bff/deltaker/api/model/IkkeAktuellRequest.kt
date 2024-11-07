package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.harEndretSluttaarsak
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBegrunnelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
    val begrunnelse: String?,
    override val forslagId: UUID?,
) : EndringsforslagRequest {
    private val kanBliIkkeAktuell = listOf(DeltakerStatus.Type.VENTER_PA_OPPSTART, DeltakerStatus.Type.IKKE_AKTUELL)

    override fun valider(deltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(deltaker.status.type in kanBliIkkeAktuell) {
            "Kan ikke sette deltaker med status ${deltaker.status.type} til ikke aktuell"
        }
        validerDeltakerKanEndres(deltaker)
        validerBegrunnelse(begrunnelse)
        require(deltakerErEndret(deltaker)) {
            "Kan ikke oppdatere deltaker som allerede er ikke aktuell med samme årsak"
        }
    }

    private fun deltakerErEndret(deltaker: Deltaker): Boolean {
        return deltaker.status.type != DeltakerStatus.Type.IKKE_AKTUELL ||
            harEndretSluttaarsak(deltaker.status.aarsak, aarsak)
    }
}
