package no.nav.amt.deltaker.bff.deltaker.navbruker.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Oppfolgingsperiode(
    val id: UUID,
    val startdato: LocalDateTime,
    val sluttdato: LocalDateTime?,
) {
    fun erAktiv(): Boolean {
        val now = LocalDate.now()
        return !(now.isBefore(startdato.toLocalDate()) || (sluttdato != null && !now.isBefore(sluttdato.toLocalDate())))
    }
}
