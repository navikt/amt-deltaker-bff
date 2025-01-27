package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import java.time.LocalDate

data class DeltakerlisteResponse(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val apentForPamelding: Boolean,
    val antallPlasser: Int?,
)
