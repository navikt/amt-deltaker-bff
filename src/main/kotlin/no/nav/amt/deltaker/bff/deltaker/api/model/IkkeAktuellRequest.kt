package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
) {
    // her kommer det antageligvis en oppdateringnår vi lander hvor lenge man skal kunne sette deltaker til ikke aktuell
    // etter at statusen blir "deltar"
    private val kanBliIkkeAktuell = listOf(DeltakerStatus.Type.VENTER_PA_OPPSTART, DeltakerStatus.Type.DELTAR)

    fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(opprinneligDeltaker.status.type in kanBliIkkeAktuell) {
            "Kan ikke sette deltaker med status ${opprinneligDeltaker.status.type} til ikke aktuell"
        }
    }
}
