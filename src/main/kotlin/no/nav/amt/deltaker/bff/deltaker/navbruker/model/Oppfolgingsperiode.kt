package no.nav.amt.deltaker.bff.deltaker.navbruker.model

import java.time.LocalDateTime
import java.util.UUID

data class Oppfolgingsperiode(
    val id: UUID,
    val startdato: LocalDateTime,
    val sluttdato: LocalDateTime?,
)
