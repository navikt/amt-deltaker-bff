package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Innhold
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerResponse(
    val deltakerId: UUID,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val deltakerliste: DeltakerlisteDto,
    val status: DeltakerStatus,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val vedtaksinformasjon: VedtaksinformasjonDto?,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String,
    val sistEndretAvEnhet: String?,
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
}

data class DeltakerlisteDto(
    val deltakerlisteId: UUID,
    val deltakerlisteNavn: String,
    val tiltakstype: Tiltak.Type,
    val arrangorNavn: String,
    val oppstartstype: Deltakerliste.Oppstartstype,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
)

fun Deltaker.toDeltakerResponse(): DeltakerResponse {
    return this.toDeltakerResponse(emptyMap())
}

fun Deltaker.toDeltakerResponse(
    ansatte: Map<String, NavAnsatt>,
    vedtakSistEndretAvEnhet: NavEnhet? = null,
): DeltakerResponse {
    return DeltakerResponse(
        deltakerId = id,
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        deltakerliste = DeltakerlisteDto(
            deltakerlisteId = deltakerliste.id,
            deltakerlisteNavn = deltakerliste.navn,
            tiltakstype = deltakerliste.tiltak.type,
            arrangorNavn = deltakerliste.arrangor.navn,
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
        innhold = innhold,
        vedtaksinformasjon = vedtaksinformasjon?.toDto(ansatte, vedtakSistEndretAvEnhet),
        sistEndret = sistEndret,
        sistEndretAv = ansatte[sistEndretAv]?.navn ?: sistEndretAv,
        sistEndretAvEnhet = sistEndretAvEnhet,
    )
}

fun Deltaker.Vedtaksinformasjon.toDto(ansatte: Map<String, NavAnsatt>, vedtakSistEndretEnhet: NavEnhet?) =
    DeltakerResponse.VedtaksinformasjonDto(
        fattet = fattet,
        fattetAvNav = fattetAvNav?.let { true } ?: false,
        opprettet = opprettet,
        opprettetAv = ansatte[opprettetAv]?.navn ?: opprettetAv,
        sistEndret = sistEndret,
        sistEndretAv = ansatte[sistEndretAv]?.navn ?: sistEndretAv,
        sistEndretAvEnhet = vedtakSistEndretEnhet?.navn ?: sistEndretAvEnhet,

    )
