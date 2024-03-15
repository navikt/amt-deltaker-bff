package no.nav.amt.deltaker.bff.innbygger.model

import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InnbyggerDeltakerResponse(
    val deltakerId: UUID,
    val deltakerliste: DeltakerlisteDto,
    val status: DeltakerStatus,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: DeltakelsesinnholdDto?,
    val vedtaksinformasjon: VedtaksinformasjonDto?,
    val adresseDelesMedArrangor: Boolean,
) {
    data class VedtaksinformasjonDto(
        val fattet: LocalDateTime?,
        val fattetAvNav: Boolean,
        val opprettet: LocalDateTime,
        val opprettetAv: String,
        val sistEndret: LocalDateTime,
        val sistEndretAv: String,
        val sistEndretAvEnhet: String?,
    )

    data class DeltakelsesinnholdDto(
        val ledetekst: String,
        val innhold: List<Innhold>,
    )

    data class DeltakerlisteDto(
        val deltakerlisteId: UUID,
        val deltakerlisteNavn: String,
        val tiltakstype: Tiltak.Type,
        val arrangorNavn: String,
        val oppstartstype: Deltakerliste.Oppstartstype,
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
    )
}

fun Deltaker.toInnbyggerDeltakerResponse(ansatte: Map<UUID, NavAnsatt>, vedtakSistEndretAvEnhet: NavEnhet?): InnbyggerDeltakerResponse {
    return InnbyggerDeltakerResponse(
        deltakerId = id,
        deltakerliste = InnbyggerDeltakerResponse.DeltakerlisteDto(
            deltakerlisteId = deltakerliste.id,
            deltakerlisteNavn = deltakerliste.navn,
            tiltakstype = deltakerliste.tiltak.type,
            arrangorNavn = deltakerliste.arrangor.getArrangorNavn(),
            oppstartstype = deltakerliste.getOppstartstype(),
            startdato = deltakerliste.startDato,
            sluttdato = deltakerliste.sluttDato,
        ),
        status = status,
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        deltakelsesinnhold = deltakerliste.tiltak.innhold?.let {
            InnbyggerDeltakerResponse.DeltakelsesinnholdDto(
                ledetekst = it.ledetekst,
                innhold = innhold,
            )
        },
        vedtaksinformasjon = vedtaksinformasjon?.toDto(ansatte, vedtakSistEndretAvEnhet),
        adresseDelesMedArrangor = adresseDelesMedArrangor(),
    )
}

private fun Vedtak.toDto(ansatte: Map<UUID, NavAnsatt>, vedtakSistEndretEnhet: NavEnhet?) = InnbyggerDeltakerResponse.VedtaksinformasjonDto(
    fattet = fattet,
    fattetAvNav = fattetAvNav,
    opprettet = opprettet,
    opprettetAv = ansatte[opprettetAv]?.navn ?: opprettetAv.toString(),
    sistEndret = sistEndret,
    sistEndretAv = ansatte[sistEndretAv]?.navn ?: sistEndretAv.toString(),
    sistEndretAvEnhet = vedtakSistEndretEnhet?.navn ?: sistEndretAvEnhet.toString(),
)
