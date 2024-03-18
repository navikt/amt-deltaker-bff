package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate

data class ForlengDeltakelseRequest(
    val sluttdato: LocalDate,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        require(!nySluttdatoErTidligereEnnForrigeSluttdato(opprinneligDeltaker)) {
            "Ny sluttdato må være etter opprinnelig sluttdato ved forlengelse"
        }
        validerSluttdatoForDeltaker(sluttdato, opprinneligDeltaker.startdato, opprinneligDeltaker)
        require(deltakerDeltarEllerHarSluttet(opprinneligDeltaker)) {
            "Kan ikke forlenge deltakelse for deltaker med status ${opprinneligDeltaker.status.type}"
        }
        require(!deltakerHarSluttetForMerEnnToMndSiden(opprinneligDeltaker)) {
            "Kan ikke forlenge deltakelse for deltaker som sluttet for mer enn to måneder siden"
        }
    }

    private fun nySluttdatoErTidligereEnnForrigeSluttdato(opprinneligDeltaker: Deltaker) =
        opprinneligDeltaker.sluttdato != null && opprinneligDeltaker.sluttdato.isAfter(sluttdato)

    private fun deltakerDeltarEllerHarSluttet(opprinneligDeltaker: Deltaker) =
        opprinneligDeltaker.status.type == DeltakerStatus.Type.DELTAR || opprinneligDeltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET

    private fun deltakerHarSluttetForMerEnnToMndSiden(opprinneligDeltaker: Deltaker): Boolean {
        if (opprinneligDeltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET) {
            return opprinneligDeltaker.sluttdato?.isBefore(LocalDate.now().minusMonths(2)) == true
        }
        return false
    }
}
