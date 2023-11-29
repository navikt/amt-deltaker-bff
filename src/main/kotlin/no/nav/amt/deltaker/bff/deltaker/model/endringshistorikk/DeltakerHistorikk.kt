package no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk

import java.time.LocalDateTime
import java.util.UUID

data class DeltakerHistorikk(
    val id: UUID,
    val deltakerId: UUID,
    val endringType: DeltakerEndringType,
    val endring: DeltakerEndring,
    val endretAv: String,
    val endret: LocalDateTime,
)
