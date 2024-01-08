package no.nav.amt.deltaker.bff.deltaker.model.deltakerendring

import java.time.LocalDateTime
import java.util.UUID

data class DeltakerEndring(
    val id: UUID,
    val deltakerId: UUID,
    val endringstype: Endringstype,
    val endring: Endring,
    val endretAv: String,
    val endretAvEnhet: String?,
    val endret: LocalDateTime,
)
