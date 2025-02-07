package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteResponse(
    val id: UUID,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
    val koordinatorer: List<KoordinatorResponse>
)
