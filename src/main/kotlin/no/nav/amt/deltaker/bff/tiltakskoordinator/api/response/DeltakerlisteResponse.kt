package no.nav.amt.deltaker.bff.tiltakskoordinator.api.response

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteResponse(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakstype.Tiltakskode,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
    val koordinatorer: List<KoordinatorResponse>,
)
