package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.utils.toTitleCase
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
    val deltakelsesinnhold: DeltakelsesinnholdDto?,
    val vedtaksinformasjon: VedtaksinformasjonDto?,
    val adresseDelesMedArrangor: Boolean,
    val kanEndres: Boolean,
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

fun Deltaker.toDeltakerResponse(ansatte: Map<UUID, NavAnsatt>, vedtakSistEndretAvEnhet: NavEnhet?): DeltakerResponse {
    return DeltakerResponse(
        deltakerId = id,
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        deltakerliste = DeltakerResponse.DeltakerlisteDto(
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
            DeltakerResponse.DeltakelsesinnholdDto(
                ledetekst = it.ledetekst,
                innhold = fulltInnhold(innhold, it.innholdselementer),
            )
        },
        vedtaksinformasjon = vedtaksinformasjon?.toDto(ansatte, vedtakSistEndretAvEnhet),
        adresseDelesMedArrangor = adresseDelesMedArrangor(),
        kanEndres = kanEndres,
    )
}

fun Deltakerliste.Arrangor.getArrangorNavn(): String {
    val arrangorNavnForDeltakerliste =
        if (overordnetArrangorNavn.isNullOrEmpty() || overordnetArrangorNavn == "Ukjent Virksomhet") {
            arrangor.navn
        } else {
            overordnetArrangorNavn
        }
    return toTitleCase(arrangorNavnForDeltakerliste)
}

fun fulltInnhold(valgtInnhold: List<Innhold>, innholdselementer: List<Innholdselement>): List<Innhold> {
    return innholdselementer
        .asSequence()
        .filter { it.innholdskode !in valgtInnhold.map { vi -> vi.innholdskode } }
        .map { it.toInnhold() }
        .plus(valgtInnhold)
        .sortedWith(sortertAlfabetiskMedAnnetSist())
        .toList()
}

private fun sortertAlfabetiskMedAnnetSist() = compareBy<Innhold> {
    it.tekst == annetInnholdselement.tekst
}.thenBy {
    it.tekst
}

private fun Vedtak.toDto(ansatte: Map<UUID, NavAnsatt>, vedtakSistEndretEnhet: NavEnhet?) = DeltakerResponse.VedtaksinformasjonDto(
    fattet = fattet,
    fattetAvNav = fattetAvNav,
    opprettet = opprettet,
    opprettetAv = ansatte[opprettetAv]?.navn ?: opprettetAv.toString(),
    sistEndret = sistEndret,
    sistEndretAv = ansatte[sistEndretAv]?.navn ?: sistEndretAv.toString(),
    sistEndretAvEnhet = vedtakSistEndretEnhet?.navn ?: sistEndretAvEnhet.toString(),
)
