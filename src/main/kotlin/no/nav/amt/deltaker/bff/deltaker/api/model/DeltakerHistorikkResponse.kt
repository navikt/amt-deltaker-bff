package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.FattetAvNav
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltakerliste.Innhold
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import java.time.LocalDateTime

sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
    val endringstype: DeltakerEndring.Endringstype,
    val endring: DeltakerEndring.Endring,
    val endretAv: String,
    val endretAvEnhet: String?,
    val endret: LocalDateTime,
) : DeltakerHistorikkResponse

data class VedtakResponse(
    val fattet: LocalDateTime?,
    val bakgrunnsinformasjon: String?,
    val fattetAvNav: FattetAvNav?,
    val innhold: List<Innhold>,
    val opprettetAv: String,
    val opprettetAvEnhet: String?,
    val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

fun List<DeltakerHistorikk>.toResponse(ansatte: Map<String, NavAnsatt>): List<DeltakerHistorikkResponse> {
    return this.map {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toResponse(ansatte)
            is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse(ansatte)
        }
    }
}

fun DeltakerEndring.toResponse(ansatte: Map<String, NavAnsatt>) = DeltakerEndringResponse(
    endringstype = endringstype,
    endring = endring,
    endretAv = ansatte[endretAv]?.navn ?: endretAv,
    endretAvEnhet = endretAvEnhet,
    endret = endret,
)

fun Vedtak.toResponse(ansatte: Map<String, NavAnsatt>) = VedtakResponse(
    fattet = fattet,
    bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
    innhold = deltakerVedVedtak.innhold,
    fattetAvNav = fattetAvNav?.let { FattetAvNav(ansatte[it.fattetAv]?.navn ?: it.fattetAv, it.fattetAvEnhet) },
    opprettetAv = ansatte[opprettetAv]?.navn ?: opprettetAv,
    opprettetAvEnhet = opprettetAvEnhet,
    opprettet = opprettet,
)
