package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import java.time.LocalDateTime
import java.util.UUID

sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
    val endringstype: DeltakerEndring.Endringstype,
    val endring: DeltakerEndring.Endring,
    val endretAv: String,
    val endretAvEnhet: UUID,
    val endret: LocalDateTime,
) : DeltakerHistorikkResponse

data class VedtakResponse(
    val fattet: LocalDateTime?,
    val bakgrunnsinformasjon: String?,
    val fattetAvNav: Boolean,
    val innhold: List<Innhold>,
    val opprettetAv: String,
    val opprettetAvEnhet: UUID,
    val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

fun List<DeltakerHistorikk>.toResponse(ansatte: Map<UUID, NavAnsatt>): List<DeltakerHistorikkResponse> {
    return this.map {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toResponse(ansatte)
            is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse(ansatte)
        }
    }
}

fun DeltakerEndring.toResponse(ansatte: Map<UUID, NavAnsatt>) = DeltakerEndringResponse(
    endringstype = endringstype,
    endring = endring,
    endretAv = ansatte[endretAv]!!.navn,
    endretAvEnhet = endretAvEnhet,
    endret = endret,
)

fun Vedtak.toResponse(ansatte: Map<UUID, NavAnsatt>) = VedtakResponse(
    fattet = fattet,
    bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
    innhold = deltakerVedVedtak.innhold,
    fattetAvNav = fattetAvNav,
    opprettetAv = ansatte[opprettetAv]!!.navn,
    opprettetAvEnhet = opprettetAvEnhet,
    opprettet = opprettet,
)
