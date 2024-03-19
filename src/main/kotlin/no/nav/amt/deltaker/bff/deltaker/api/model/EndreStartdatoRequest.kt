package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import java.time.LocalDate

data class EndreStartdatoRequest(
    val startdato: LocalDate?,
    val sluttdato: LocalDate? = null,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerDeltakerKanEndres(opprinneligDeltaker)
        require(startdato == null || !startdato.isBefore(opprinneligDeltaker.deltakerliste.startDato)) {
            "Startdato kan ikke være tidligere enn deltakerlistens startdato"
        }
        sluttdato?.let { validerSluttdatoForDeltaker(it, startdato, opprinneligDeltaker) }
    }
}
