package no.nav.amt.deltaker.bff.deltaker.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.deltaker.bff.deltaker.model.Deltakelsesinnhold
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DeltakerEndringResponse::class, name = "Endring"),
    JsonSubTypes.Type(value = VedtakResponse::class, name = "Vedtak"),
    JsonSubTypes.Type(value = ForslagResponse::class, name = "Forslag"),
)
sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
    val endring: DeltakerEndring.Endring,
    val endretAv: String,
    val endretAvEnhet: String,
    val endret: LocalDateTime,
    val forslag: Forslag?,
) : DeltakerHistorikkResponse {
    data class Forslag(
        val opprettet: LocalDateTime,
        val begrunnelseFraArrangor: String?,
        val arrangorNavn: String,
        val endring: no.nav.amt.lib.models.arrangor.melding.Forslag.Endring,
    )
}

data class VedtakResponse(
    val fattet: LocalDateTime?,
    val bakgrunnsinformasjon: String?,
    val fattetAvNav: Boolean,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val opprettetAv: String,
    val opprettetAvEnhet: String,
    val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

data class ForslagResponse(
    val opprettet: LocalDateTime,
    val begrunnelseFraArrangor: String?,
    val arrangorNavn: String,
    val endring: Forslag.Endring,
    val status: ForslagResponseStatus,
) : DeltakerHistorikkResponse

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface ForslagResponseStatus {
    data class Avvist(
        val avvistAv: String,
        val avvistAvEnhet: String,
        val avvist: LocalDateTime,
        val begrunnelseFraNav: String,
    ) : ForslagResponseStatus

    data class Tilbakekalt(
        val tilbakekalt: LocalDateTime,
    ) : ForslagResponseStatus

    data class Erstattet(
        val erstattet: LocalDateTime,
    ) : ForslagResponseStatus
}

fun List<DeltakerHistorikk>.toResponse(
    ansatte: Map<UUID, NavAnsatt>,
    arrangornavn: String,
    enheter: Map<UUID, NavEnhet>,
): List<DeltakerHistorikkResponse> = this.map {
    when (it) {
        is DeltakerHistorikk.Endring -> it.endring.toResponse(ansatte, enheter, arrangornavn)
        is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse(ansatte, enheter)
        is DeltakerHistorikk.Forslag -> it.forslag.toResponse(arrangornavn, ansatte, enheter)
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
    forslag = forslag?.toDeltakeEndringForslag(arrangornavn),
)

fun Vedtak.toResponse(ansatte: Map<UUID, NavAnsatt>, enheter: Map<UUID, NavEnhet>) = VedtakResponse(
    fattet = fattet,
    bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
    deltakelsesinnhold = deltakerVedVedtak.deltakelsesinnhold,
    fattetAvNav = fattetAvNav,
    opprettetAv = ansatte[opprettetAv]!!.navn,
    opprettetAvEnhet = enheter[opprettetAvEnhet]!!.navn,
    opprettet = opprettet,
)

fun Forslag.toResponse(
    arrangornavn: String,
    ansatte: Map<UUID, NavAnsatt>,
    enheter: Map<UUID, NavEnhet>,
): ForslagResponse = ForslagResponse(
    opprettet = opprettet,
    begrunnelseFraArrangor = begrunnelse,
    arrangorNavn = arrangornavn,
    endring = endring,
    status = getForslagResponseStatus(ansatte, enheter),
)

private fun Forslag.toDeltakeEndringForslag(arrangornavn: String) = DeltakerEndringResponse.Forslag(
    opprettet = opprettet,
    begrunnelseFraArrangor = begrunnelse,
    arrangorNavn = arrangornavn,
    endring = endring,
)

private fun Forslag.getForslagResponseStatus(ansatte: Map<UUID, NavAnsatt>, enheter: Map<UUID, NavEnhet>): ForslagResponseStatus =
    when (val status = status) {
        is Forslag.Status.VenterPaSvar,
        is Forslag.Status.Godkjent,
        -> throw IllegalStateException("Ulovlig status for forslag i deltakerhistorikk for deltaker $deltakerId")

        is Forslag.Status.Avvist -> {
            val avvist = status
            ForslagResponseStatus.Avvist(
                avvistAv = ansatte[avvist.avvistAv.id]!!.navn,
                avvistAvEnhet = enheter[avvist.avvistAv.enhetId]!!.navn,
                avvist = avvist.avvist,
                begrunnelseFraNav = avvist.begrunnelseFraNav,
            )
        }

        is Forslag.Status.Tilbakekalt -> ForslagResponseStatus.Tilbakekalt(status.tilbakekalt)
        is Forslag.Status.Erstattet -> ForslagResponseStatus.Erstattet(status.erstattet)
    }
