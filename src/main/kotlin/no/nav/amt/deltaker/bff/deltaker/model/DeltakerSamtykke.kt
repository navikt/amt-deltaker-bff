package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltaker.Deltaker
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerSamtykke(
    val id: UUID,
    val deltakerId: UUID,
    val godkjent: LocalDateTime?,
    val gyldigTil: LocalDateTime?,
    val deltakerVedSamtykke: Deltaker,
    val godkjentAvNav: GodkjenningAvNav?,
)

data class GodkjenningAvNav(
    val type: String,
    val beskrivelse: String?,
    val godkjentAv: String,
)
