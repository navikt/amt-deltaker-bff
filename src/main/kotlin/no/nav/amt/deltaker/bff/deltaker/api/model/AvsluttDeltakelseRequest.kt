package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerAarsaksBeskrivelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBegrunnelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerSluttdatoForDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.time.LocalDate
import java.util.UUID

data class AvsluttDeltakelseRequest(
    val aarsak: DeltakerEndring.Aarsak,
    val sluttdato: LocalDate?,
    val harDeltatt: Boolean? = true,
    val begrunnelse: String?,
    override val forslagId: UUID?,
) : EndringsforslagRequest {
    private val kanEndreAvslutteDeltakelse = listOf(DeltakerStatus.Type.DELTAR, DeltakerStatus.Type.HAR_SLUTTET)

    override fun valider(deltaker: Deltaker) {
        validerAarsaksBeskrivelse(aarsak.beskrivelse)
        validerBegrunnelse(begrunnelse)
        require(deltaker.status.type in kanEndreAvslutteDeltakelse) {
            "Kan ikke avslutte deltakelse for deltaker som ikke har status DELTAR eller HAR_SLUTTET"
        }
        if (harDeltatt()) {
            require(sluttdato != null) {
                "Må angi sluttdato for deltaker som har deltatt"
            }
        } else {
            require(deltaker.status.type == DeltakerStatus.Type.DELTAR) {
                "Deltaker som ikke har status DELTAR må ha deltatt"
            }
            require(statusForMindreEnn15DagerSiden(deltaker)) {
                "Deltaker med deltar-status mer enn 15 dager tilbake i tid må ha deltatt"
            }
        }
        sluttdato?.let { validerSluttdatoForDeltaker(it, deltaker.startdato, deltaker) }
    }

    fun harDeltatt(): Boolean = harDeltatt == null || harDeltatt == true
}

fun statusForMindreEnn15DagerSiden(opprinneligDeltaker: Deltaker): Boolean = opprinneligDeltaker.status.gyldigFra
    .toLocalDate()
    .isAfter(LocalDate.now().minusDays(15))
