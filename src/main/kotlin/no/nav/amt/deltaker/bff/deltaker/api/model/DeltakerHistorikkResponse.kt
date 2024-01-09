package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.GodkjenningAvNav
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import java.time.LocalDateTime

sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
    val endringstype: DeltakerEndring.Endringstype,
    val endring: DeltakerEndring.Endring,
    val endretAv: String,
    val endretAvEnhet: String?,
    val endret: LocalDateTime,
) : DeltakerHistorikkResponse

data class DeltakerSamtykkeResponse(
    val godkjent: LocalDateTime?,
    val bakgrunnsinformasjon: String?,
    val mal: List<Mal>,
    val godkjentAvNav: GodkjenningAvNav?,
    val opprettetAv: String,
    val opprettetAvEnhet: String?,
    val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

fun List<DeltakerHistorikk>.toResponse(): List<DeltakerHistorikkResponse> {
    return this.map {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toResponse()
            is DeltakerHistorikk.Samtykke -> it.samtykke.toResponse()
        }
    }
}

fun DeltakerEndring.toResponse() = DeltakerEndringResponse(
    endringstype = endringstype,
    endring = endring,
    endretAv = endretAv,
    endretAvEnhet = endretAvEnhet,
    endret = endret,
)

fun DeltakerSamtykke.toResponse() = DeltakerSamtykkeResponse(
    godkjent = godkjent,
    bakgrunnsinformasjon = deltakerVedSamtykke.bakgrunnsinformasjon,
    mal = deltakerVedSamtykke.mal,
    godkjentAvNav = godkjentAvNav,
    opprettetAv = opprettetAv,
    opprettetAvEnhet = opprettetAvEnhet,
    opprettet = opprettet,
)
