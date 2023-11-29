package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
    val id: UUID,
    val personident: String,
    val deltakerlisteId: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val mal: List<Mal>,
    val status: DeltakerStatus,
    val sistEndretAv: String,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
) {
    fun harSluttet(): Boolean {
        return status.type in AVSLUTTENDE_STATUSER || sluttdato?.isBefore(LocalDate.now().minusWeeks(2)) == true
    }
}
