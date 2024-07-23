package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate

data class EndreStartdatoRequest(
    val startdato: LocalDate?,
    val sluttdato: LocalDate? = null,
) : Endringsrequest {
    private val kanEndreStartdato =
        listOf(DeltakerStatus.Type.VENTER_PA_OPPSTART, DeltakerStatus.Type.DELTAR, DeltakerStatus.Type.HAR_SLUTTET)

    override fun valider(opprinneligDeltaker: Deltaker) {
        validerDeltakerKanEndres(opprinneligDeltaker)
        require(opprinneligDeltaker.status.type in kanEndreStartdato) {
            "Kan ikke endre startdato for deltaker med status ${opprinneligDeltaker.status.type}"
        }
        require(startdato == null || !startdato.isBefore(opprinneligDeltaker.deltakerliste.startDato)) {
            "Startdato kan ikke være tidligere enn deltakerlistens startdato"
        }
        sluttdato?.let { validerSluttdatoForDeltaker(it, startdato, opprinneligDeltaker) }
    }
}
