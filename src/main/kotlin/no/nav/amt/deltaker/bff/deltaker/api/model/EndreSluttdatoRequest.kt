package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate

data class EndreSluttdatoRequest(
    val sluttdato: LocalDate,
) {
    private val kanEndreSluttdato = listOf(DeltakerStatus.Type.HAR_SLUTTET, DeltakerStatus.Type.AVBRUTT, DeltakerStatus.Type.FULLFORT)

    fun valider(opprinneligDeltaker: Deltaker) {
        require(opprinneligDeltaker.status.type in kanEndreSluttdato) {
            "Kan ikke endre sluttdato for deltaker som ikke har sluttet"
        }
        validerSluttdatoForDeltaker(sluttdato, opprinneligDeltaker.startdato, opprinneligDeltaker)
    }
}
