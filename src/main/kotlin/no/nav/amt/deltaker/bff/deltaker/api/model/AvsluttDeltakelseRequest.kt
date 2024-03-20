package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate

data class AvsluttDeltakelseRequest(
    val aarsak: DeltakerEndring.Aarsak,
    val sluttdato: LocalDate,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(opprinneligDeltaker.status.type == DeltakerStatus.Type.DELTAR) {
            "Kan ikke avslutte deltakelse for deltaker som ikke har status DELTAR"
        }
        validerSluttdatoForDeltaker(sluttdato, opprinneligDeltaker.startdato, opprinneligDeltaker)
    }
}
