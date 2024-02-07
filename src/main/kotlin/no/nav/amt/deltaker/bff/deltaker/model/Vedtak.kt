package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDateTime
import java.util.UUID

data class Vedtak(
    val id: UUID,
    val deltakerId: UUID,
    val fattet: LocalDateTime?,
    val gyldigTil: LocalDateTime?,
    val deltakerVedVedtak: Deltaker,
    val fattetAvNav: FattetAvNav?,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val opprettetAvEnhet: String?,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String,
    val sistEndretAvEnhet: String?,
)

data class FattetAvNav(
    val fattetAv: String,
    val fattetAvEnhet: String?,
)
