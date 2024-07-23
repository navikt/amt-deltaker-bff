package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.time.LocalDate
import java.util.UUID

data class AvsluttDeltakelseRequest(
    val aarsak: DeltakerEndring.Aarsak,
    val sluttdato: LocalDate?,
    val harDeltatt: Boolean? = true,
    val begrunnelse: String?,
    override val forslagId: UUID?,
) : EndringsforslagRequest {
    override fun valider(opprinneligDeltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        require(opprinneligDeltaker.status.type == DeltakerStatus.Type.DELTAR) {
            "Kan ikke avslutte deltakelse for deltaker som ikke har status DELTAR"
        }
        if (harDeltatt()) {
            require(sluttdato != null) {
                "Må angi sluttdato for deltaker som har deltatt"
            }
        } else {
            require(statusForMindreEnn15DagerSiden(opprinneligDeltaker)) {
                "Deltaker med deltar-status mer enn 15 dager tilbake i tid må ha deltatt"
            }
        }
        sluttdato?.let { validerSluttdatoForDeltaker(it, opprinneligDeltaker.startdato, opprinneligDeltaker) }
    }

    fun harDeltatt(): Boolean = harDeltatt == null || harDeltatt == true
}

fun statusForMindreEnn15DagerSiden(opprinneligDeltaker: Deltaker): Boolean = opprinneligDeltaker.status.gyldigFra
    .toLocalDate()
    .isAfter(LocalDate.now().minusDays(15))
