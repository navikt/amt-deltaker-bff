package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import java.time.LocalDate

data class EndreStartdatoRequest(
    val startdato: LocalDate?,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke endre startdato for deltaker som har sluttet"
        }
    }
}
