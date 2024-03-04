package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Vedtak(
    val id: UUID,
    val deltakerId: UUID,
    val fattet: LocalDateTime?,
    val gyldigTil: LocalDateTime?,
    val deltakerVedVedtak: DeltakerVedVedtak,
    val fattetAvNav: FattetAvNav?,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val opprettetAvEnhet: String?,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String,
    val sistEndretAvEnhet: String?,
)

data class DeltakerVedVedtak(
    val id: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val status: DeltakerStatus,
)

data class FattetAvNav(
    val fattetAv: String,
    val fattetAvEnhet: String?,
)

data class VedtakDbo(
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
