package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate

data class EndreSluttdatoRequest(
    val sluttdato: LocalDate,
) : Endringsrequest {
    private val kanEndreSluttdato = listOf(DeltakerStatus.Type.HAR_SLUTTET, DeltakerStatus.Type.AVBRUTT, DeltakerStatus.Type.FULLFORT)

    override fun valider(deltaker: Deltaker) {
        validerDeltakerKanEndres(deltaker)
        require(deltaker.status.type in kanEndreSluttdato) {
            "Kan ikke endre sluttdato for deltaker som ikke har sluttet"
        }
        validerSluttdatoForDeltaker(sluttdato, deltaker.startdato, deltaker)
    }
}
