package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.getInnholdselementerMedAnnet
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.toInnhold
import no.nav.amt.deltaker.bff.utils.toTitleCase
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.models.deltaker.Vedtak
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.Deltakelsesmengde
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.models.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.models.person.NavAnsatt
import no.nav.amt.lib.models.person.NavEnhet
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
    val adresseDelesMedArrangor: Boolean,
    val kanEndres: Boolean,
    val digitalBruker: Boolean,
    val maxVarighet: Long?,
    val softMaxVarighet: Long?,
    val forslag: List<ForslagResponse>,
    val vedtaksinformasjon: VedtaksinformasjonDto?,
    val importertFraArena: ImportertFraArenaDto?,
    val harAdresse: Boolean,
    val deltakelsesmengder: DeltakelsesmengderDto,
    val erUnderOppfolging: Boolean,
    val erManueltDeltMedArrangor: Boolean,
) {
    data class VedtaksinformasjonDto(
        val fattet: LocalDateTime?,
        val fattetAvNav: Boolean,
        val opprettet: LocalDateTime,
        val opprettetAv: String,
        val sistEndret: LocalDateTime,
        val sistEndretAv: String,
        val sistEndretAvEnhet: String?,
    ) {
        companion object {
            fun fromVedtak(
                vedtak: Vedtak,
                ansatte: Map<UUID, NavAnsatt>,
                vedtakSistEndretEnhet: NavEnhet?,
            ) = with(vedtak) {
                VedtaksinformasjonDto(
                    fattet = fattet,
                    fattetAvNav = fattetAvNav,
                    opprettet = opprettet,
                    opprettetAv = ansatte[opprettetAv]?.navn ?: opprettetAv.toString(),
                    sistEndret = sistEndret,
                    sistEndretAv = ansatte[sistEndretAv]?.navn ?: sistEndretAv.toString(),
                    sistEndretAvEnhet = vedtakSistEndretEnhet?.navn ?: sistEndretAvEnhet.toString(),
                )
            }
        }
    }

    data class DeltakelsesinnholdDto(
        val ledetekst: String?,
        val innhold: List<Innhold>,
    ) {
        companion object {
            fun fromDeltakelsesinnhold(deltakelsesinnhold: Deltakelsesinnhold, tiltaksInnhold: List<Innholdselement>?) =
                DeltakelsesinnholdDto(
                    ledetekst = deltakelsesinnhold.ledetekst,
                    innhold = fulltInnhold(deltakelsesinnhold.innhold, tiltaksInnhold ?: emptyList()),
                )
        }
    }

    data class DeltakerlisteDto(
        val deltakerlisteId: UUID,
        val deltakerlisteNavn: String,
        val tiltakstype: ArenaKode,
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
    ) {
        companion object {
            // litt spesiell konstruksjon med nullable innhold, undersøk nærmere
            fun fromDeltakerRegistreringInnhold(innhold: DeltakerRegistreringInnhold?, tiltakstype: Tiltakskode) = TilgjengeligInnhold(
                ledetekst = innhold?.ledetekst,
                innhold = innhold?.getInnholdselementerMedAnnet(tiltakstype).orEmpty(),
            )
        }
    }

    data class DeltakelsesmengderDto(
        val nesteDeltakelsesmengde: DeltakelsesmengdeDto?,
        val sisteDeltakelsesmengde: DeltakelsesmengdeDto?,
    )

    data class DeltakelsesmengdeDto(
        val deltakelsesprosent: Float,
        val dagerPerUke: Float?,
        val gyldigFra: LocalDate,
    ) {
        companion object {
            fun fromDeltakelsesmengde(deltakelsesmengde: Deltakelsesmengde) = with(deltakelsesmengde) {
                DeltakelsesmengdeDto(
                    deltakelsesprosent = deltakelsesprosent,
                    dagerPerUke = dagerPerUke,
                    gyldigFra = gyldigFra,
                )
            }
        }
    }

    companion object {
        fun fromDeltaker(
            deltaker: Deltaker,
            ansatte: Map<UUID, NavAnsatt>,
            vedtakSistEndretAvEnhet: NavEnhet?,
            digitalBruker: Boolean,
            forslag: List<Forslag>,
        ) = with(deltaker) {
            DeltakerResponse(
                deltakerId = id,
                fornavn = navBruker.fornavn,
                mellomnavn = navBruker.mellomnavn,
                etternavn = navBruker.etternavn,
                deltakerliste = DeltakerlisteDto(
                    deltakerlisteId = deltakerliste.id,
                    deltakerlisteNavn = deltakerliste.navn,
                    tiltakstype = deltakerliste.tiltak.arenaKode,
                    arrangorNavn = deltakerliste.arrangor.getArrangorNavn(),
                    oppstartstype = deltakerliste.getOppstartstype(),
                    startdato = deltakerliste.startDato,
                    sluttdato = deltakerliste.sluttDato,
                    status = deltakerliste.status,
                    tilgjengeligInnhold = TilgjengeligInnhold.fromDeltakerRegistreringInnhold(
                        deltakerliste.tiltak.innhold,
                        deltakerliste.tiltak.tiltakskode,
                    ),
                    // tilgjengeligInnhold = deltakerliste.tiltak.innhold.toDto(deltakerliste.tiltak.tiltakskode),
                ),
                status = status,
                startdato = startdato,
                sluttdato = sluttdato,
                dagerPerUke = dagerPerUke,
                deltakelsesprosent = deltakelsesprosent,
                bakgrunnsinformasjon = bakgrunnsinformasjon,
                deltakelsesinnhold = deltakelsesinnhold?.let {
                    DeltakelsesinnholdDto.fromDeltakelsesinnhold(
                        it,
                        deltakerliste.tiltak.innhold?.getInnholdselementerMedAnnet(deltakerliste.tiltak.tiltakskode),
                    )
                },
                vedtaksinformasjon = vedtaksinformasjon?.let { VedtaksinformasjonDto.fromVedtak(it, ansatte, vedtakSistEndretAvEnhet) },
                adresseDelesMedArrangor = adresseDelesMedArrangor(),
                kanEndres = kanEndres,
                digitalBruker = digitalBruker,
                maxVarighet = maxVarighet?.toMillis(),
                softMaxVarighet = softMaxVarighet?.toMillis(),
                forslag = forslag.map { it.toResponse(deltakerliste.arrangor.getArrangorNavn()) },
                importertFraArena = ImportertFraArenaDto.fromDeltaker(this),
                harAdresse = navBruker.adresse != null,
                deltakelsesmengder = DeltakelsesmengderDto(
                    nesteDeltakelsesmengde = deltakelsesmengder.nesteGjeldende?.let { DeltakelsesmengdeDto.fromDeltakelsesmengde(it) },
                    sisteDeltakelsesmengde = deltakelsesmengder.lastOrNull()?.let { DeltakelsesmengdeDto.fromDeltakelsesmengde(it) },
                ),
                erUnderOppfolging = navBruker.harAktivOppfolgingsperiode,
                erManueltDeltMedArrangor = erManueltDeltMedArrangor,
            )
        }
    }
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
