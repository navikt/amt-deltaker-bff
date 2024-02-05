package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDateTime
import java.util.UUID

data class DeltakerSamtykke(
    val id: UUID,
    val deltakerId: UUID,
    val godkjent: LocalDateTime?,
    val gyldigTil: LocalDateTime?,
    val deltakerVedSamtykke: Deltaker,
    val godkjentAvNav: GodkjentAvNav?,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val opprettetAvEnhet: String?,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String,
    val sistEndretAvEnhet: String?,
)

data class GodkjentAvNav(
    val godkjentAv: String,
    val godkjentAvEnhet: String?,
)
