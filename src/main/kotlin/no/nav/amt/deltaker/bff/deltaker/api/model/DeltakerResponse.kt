package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.utils.toTitleCase
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.models.deltaker.Vedtak
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.Deltakelsesmengde
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
    val digitalBruker: Boolean,
    val maxVarighet: Long?,
    val softMaxVarighet: Long?,
    val forslag: List<ForslagResponse>,
    val importertFraArena: ImportertFraArenaDto?,
    val harAdresse: Boolean,
    val deltakelsesmengder: DeltakelsesmengderDto,
    val erUnderOppfolging: Boolean,
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
        val ledetekst: String?,
        val innhold: List<Innhold>,
    )

    data class DeltakerlisteDto(
        val deltakerlisteId: UUID,
        val deltakerlisteNavn: String,
        val tiltakstype: Tiltakstype.ArenaKode,
        val arrangorNavn: String,
        val oppstartstype: Deltakerliste.Oppstartstype,
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
        val status: Deltakerliste.Status,
        val tilgjengeligInnhold: TilgjengeligInnhold,
    )

    data class TilgjengeligInnhold(
        val ledetekst: String?,
        val innhold: List<Innholdselement>,
    )

    data class DeltakelsesmengderDto(
        val nesteDeltakelsesmengde: DeltakelsesmengdeDto?,
        val sisteDeltakelsesmengde: DeltakelsesmengdeDto?,
    )

    data class DeltakelsesmengdeDto(
        val deltakelsesprosent: Float,
        val dagerPerUke: Float?,
        val gyldigFra: LocalDate,
    )
}

data class ImportertFraArenaDto(
    val innsoktDato: LocalDate,
)

fun Deltaker.toDeltakerResponse(
    ansatte: Map<UUID, NavAnsatt>,
    vedtakSistEndretAvEnhet: NavEnhet?,
    digitalBruker: Boolean,
    forslag: List<Forslag>,
): DeltakerResponse = DeltakerResponse(
    deltakerId = id,
    fornavn = navBruker.fornavn,
    mellomnavn = navBruker.mellomnavn,
    etternavn = navBruker.etternavn,
    deltakerliste = DeltakerResponse.DeltakerlisteDto(
        deltakerlisteId = deltakerliste.id,
        deltakerlisteNavn = deltakerliste.navn,
        tiltakstype = deltakerliste.tiltak.arenaKode,
        arrangorNavn = deltakerliste.arrangor.getArrangorNavn(),
        oppstartstype = deltakerliste.getOppstartstype(),
        startdato = deltakerliste.startDato,
        sluttdato = deltakerliste.sluttDato,
        status = deltakerliste.status,
        tilgjengeligInnhold = deltakerliste.tiltak.innhold.toDto(),
    ),
    status = status,
    startdato = startdato,
    sluttdato = sluttdato,
    dagerPerUke = dagerPerUke,
    deltakelsesprosent = deltakelsesprosent,
    bakgrunnsinformasjon = bakgrunnsinformasjon,
    deltakelsesinnhold = deltakelsesinnhold?.toDto(deltakerliste.tiltak.innhold?.innholdselementerMedAnnet),
    vedtaksinformasjon = vedtaksinformasjon?.toDto(ansatte, vedtakSistEndretAvEnhet),
    adresseDelesMedArrangor = adresseDelesMedArrangor(),
    kanEndres = kanEndres,
    digitalBruker = digitalBruker,
    maxVarighet = maxVarighet?.toMillis(),
    softMaxVarighet = softMaxVarighet?.toMillis(),
    forslag = forslag.map { it.toResponse(deltakerliste.arrangor.getArrangorNavn()) },
    importertFraArena = toImporertFraArenaDto(),
    harAdresse = navBruker.adresse != null,
    deltakelsesmengder = DeltakerResponse.DeltakelsesmengderDto(
        nesteDeltakelsesmengde = deltakelsesmengder.nesteGjeldende?.toDto(),
        sisteDeltakelsesmengde = deltakelsesmengder.lastOrNull()?.toDto(),
    ),
    erUnderOppfolging = if (id == UUID.fromString("e425562e-8603-4dd9-bd55-04dea391a190")) {
        true
    } else {
        navBruker.harAktivOppfolgingsperiode()
    },
)

fun Deltaker.toImporertFraArenaDto(): ImportertFraArenaDto? =
    historikk.filterIsInstance<DeltakerHistorikk.ImportertFraArena>().firstOrNull()?.let {
        ImportertFraArenaDto(it.importertFraArena.deltakerVedImport.innsoktDato)
    }

fun Deltakelsesinnhold.toDto(tiltaksInnhold: List<Innholdselement>?) = DeltakerResponse.DeltakelsesinnholdDto(
    ledetekst = ledetekst,
    innhold = fulltInnhold(innhold, tiltaksInnhold ?: emptyList()),
)

fun Deltakerliste.Arrangor.getArrangorNavn(): String {
    val arrangorNavnForDeltakerliste =
        if (overordnetArrangorNavn.isNullOrEmpty() || overordnetArrangorNavn == "Ukjent Virksomhet") {
            arrangor.navn
        } else {
            overordnetArrangorNavn
        }
    return toTitleCase(arrangorNavnForDeltakerliste)
}

fun fulltInnhold(valgtInnhold: List<Innhold>, innholdselementer: List<Innholdselement>): List<Innhold> = innholdselementer
    .asSequence()
    .filter { it.innholdskode !in valgtInnhold.map { vi -> vi.innholdskode } }
    .map { it.toInnhold() }
    .plus(valgtInnhold)
    .sortedWith(sortertAlfabetiskMedAnnetSist())
    .toList()

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

private fun DeltakerRegistreringInnhold?.toDto() = DeltakerResponse.TilgjengeligInnhold(
    ledetekst = this?.ledetekst,
    innhold = this?.innholdselementerMedAnnet.orEmpty(),
)

private fun Deltakelsesmengde.toDto() = DeltakerResponse.DeltakelsesmengdeDto(
    deltakelsesprosent,
    dagerPerUke,
    gyldigFra,
)
