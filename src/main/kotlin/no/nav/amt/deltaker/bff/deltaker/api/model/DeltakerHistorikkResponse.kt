package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.FattetAvNav
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltakerliste.Innhold
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

fun List<DeltakerHistorikk>.toResponse(): List<DeltakerHistorikkResponse> {
    return this.map {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toResponse()
            is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse()
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

fun Vedtak.toResponse() = VedtakResponse(
    fattet = fattet,
    bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
    innhold = deltakerVedVedtak.innhold,
    fattetAvNav = fattetAvNav,
    opprettetAv = opprettetAv,
    opprettetAvEnhet = opprettetAvEnhet,
    opprettet = opprettet,
)
