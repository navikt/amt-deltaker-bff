package no.nav.amt.deltaker.bff.deltaker.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.deltaker.bff.deltaker.model.Deltakelsesinnhold
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DeltakerEndringResponse::class, name = "Endring"),
    JsonSubTypes.Type(value = VedtakResponse::class, name = "Vedtak"),
    JsonSubTypes.Type(value = ForslagResponse::class, name = "Forslag"),
    JsonSubTypes.Type(value = EndringFraArrangorResponse::class, name = "EndringFraArrangor"),
)
sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
    val endring: DeltakerEndring.Endring,
    val endretAv: String,
    val endretAvEnhet: String,
    val endret: LocalDateTime,
    val forslag: ForslagResponse?,
) : DeltakerHistorikkResponse

data class VedtakResponse(
    val fattet: LocalDateTime?,
    val bakgrunnsinformasjon: String?,
    val fattetAvNav: Boolean,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val tiltakstype: Tiltakstype.ArenaKode,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val opprettetAv: String,
    val opprettetAvEnhet: String,
    val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

data class EndringFraArrangorResponse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val arrangorNavn: String,
    val endring: EndringFraArrangor.Endring,
) : DeltakerHistorikkResponse

fun List<DeltakerHistorikk>.toResponse(
    ansatte: Map<UUID, NavAnsatt>,
    arrangornavn: String,
    enheter: Map<UUID, NavEnhet>,
    tiltakstype: Tiltakstype.ArenaKode
): List<DeltakerHistorikkResponse> = this.map {
    when (it) {
        is DeltakerHistorikk.Endring -> it.endring.toResponse(ansatte, enheter, arrangornavn)
        is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse(ansatte, enheter, tiltakstype)
        is DeltakerHistorikk.Forslag -> it.forslag.toResponse(arrangornavn, ansatte, enheter)
        is DeltakerHistorikk.EndringFraArrangor -> it.endringFraArrangor.toResponse(arrangornavn)
    }
}

fun DeltakerEndring.toResponse(
    ansatte: Map<UUID, NavAnsatt>,
    enheter: Map<UUID, NavEnhet>,
    arrangornavn: String,
) = DeltakerEndringResponse(
    endring = endring,
    endretAv = ansatte[endretAv]!!.navn,
    endretAvEnhet = enheter[endretAvEnhet]!!.navn,
    endret = endret,
    forslag = forslag?.toResponse(arrangornavn),
)

fun Vedtak.toResponse(
    ansatte: Map<UUID, NavAnsatt>,
    enheter: Map<UUID, NavEnhet>,
    tiltakstype: Tiltakstype.ArenaKode
) = VedtakResponse(
    fattet = fattet,
    bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
    deltakelsesinnhold = deltakerVedVedtak.deltakelsesinnhold,
    tiltakstype = tiltakstype,
    dagerPerUke = deltakerVedVedtak.dagerPerUke,
    deltakelsesprosent = deltakerVedVedtak.deltakelsesprosent,
    fattetAvNav = fattetAvNav,
    opprettetAv = ansatte[opprettetAv]!!.navn,
    opprettetAvEnhet = enheter[opprettetAvEnhet]!!.navn,
    opprettet = opprettet,
)

fun EndringFraArrangor.toResponse(arrangornavn: String) = EndringFraArrangorResponse(
    id = id,
    opprettet = opprettet,
    arrangorNavn = arrangornavn,
    endring = endring,
)
