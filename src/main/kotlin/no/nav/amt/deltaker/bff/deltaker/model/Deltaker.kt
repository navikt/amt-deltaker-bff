package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.Deltakelsesmengder
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.toDeltakelsesmengder
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.models.person.NavBruker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Deltaker(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerliste: Deltakerliste,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val status: DeltakerStatus,
    val historikk: List<DeltakerHistorikk>,
    val kanEndres: Boolean,
    val sistEndret: LocalDateTime,
    val erManueltDeltMedArrangor: Boolean,
) {
    val deltakelsesmengder: Deltakelsesmengder
        get() = startdato?.let { historikk.toDeltakelsesmengder().periode(it, sluttdato) } ?: historikk.toDeltakelsesmengder()

    val fattetVedtak
        get() = historikk
            .firstOrNull {
                it is DeltakerHistorikk.Vedtak && it.vedtak.gyldigTil == null && it.vedtak.fattet != null
            }?.let { (it as DeltakerHistorikk.Vedtak).vedtak }

    val paameldtDato
        get() = historikk
            .firstOrNull { it is DeltakerHistorikk.Vedtak || it is DeltakerHistorikk.ImportertFraArena }
            ?.let {
                when (it) {
                    is DeltakerHistorikk.ImportertFraArena ->
                        it.importertFraArena.deltakerVedImport.innsoktDato
                            .atStartOfDay()

                    is DeltakerHistorikk.Vedtak -> it.vedtak.fattet
                    else -> null
                }
            }

    val ikkeFattetVedtak
        get() = historikk
            .firstOrNull {
                it is DeltakerHistorikk.Vedtak && it.vedtak.fattet == null
            }?.let { (it as DeltakerHistorikk.Vedtak).vedtak }

    val vedtaksinformasjon
        get() = if (this.fattetVedtak != null) {
            fattetVedtak
        } else {
            ikkeFattetVedtak
        }

    fun getDeltakerHistorikkForVisning() = historikk
        .filterNot {
            deltakerliste.oppstart == Oppstartstype.FELLES &&
                it is DeltakerHistorikk.Vedtak
        }.sortedByDescending { it.sistEndret }

    fun harSluttet(): Boolean = status.type in AVSLUTTENDE_STATUSER

    fun harSluttetForMindreEnnToMndSiden(): Boolean {
        if (!harSluttet()) return false

        val nyesteDato = listOfNotNull(sluttdato, status.gyldigFra.toLocalDate())
            .maxOrNull() ?: return false

        val toMndSiden = LocalDate.now().minusMonths(2)
        return nyesteDato.isAfter(toMndSiden)
    }

    fun adresseDelesMedArrangor() = this.navBruker.adressebeskyttelse == null &&
        this.deltakerliste.deltakerAdresseDeles()

    /**
     Noen tiltak har en max varighet som kan overgås ved visse omstendigheter,
     softMaxVarighet er den ordinære varigheten for disse tiltakene,
     mens maxVarighet er det absolutte max for alle tiltak.

     https://confluence.adeo.no/pages/viewpage.action?pageId=583122378
     https://lovdata.no/forskrift/2015-12-11-1598
     */
    val softMaxVarighet: Duration?
        get() = when (deltakerliste.tiltak.tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> years(2)
            Tiltakskode.OPPFOLGING -> when (navBruker.innsatsgruppe) {
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                -> years(3)

                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Innsatsgruppe.STANDARD_INNSATS,
                null,
                -> null
            }

            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.AVKLARING,
            -> weeks(12)

            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> weeks(8)
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.JOBBKLUBB,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            // nye tiltakskoder
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
            -> null
        }

    /**
     Noen tiltak har en max varighet som kan overgås ved visse omstendigheter,
     softMaxVarighet er den ordinære varigheten for disse tiltakene,
     mens maxVarighet er det absolutte max for alle tiltak.

     https://confluence.adeo.no/pages/viewpage.action?pageId=583122378
     https://lovdata.no/forskrift/2015-12-11-1598
     */
    val maxVarighet: Duration?
        get() = when (deltakerliste.tiltak.tiltakskode) {
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            -> years(3)

            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.AVKLARING,
            -> weeks(12 + FERIETILLEGG)

            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> weeks(8 + FERIETILLEGG)
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> years(4)

            Tiltakskode.OPPFOLGING -> when (navBruker.innsatsgruppe) {
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> years(1)
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                -> years(3).plus(months(6))

                else -> null
            }

            Tiltakskode.JOBBKLUBB,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,

            // nye tiltakskoder
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
            -> null
        }

    fun oppdater(oppdatering: Deltakeroppdatering) = this.copy(
        startdato = oppdatering.startdato,
        sluttdato = oppdatering.sluttdato,
        dagerPerUke = oppdatering.dagerPerUke,
        deltakelsesprosent = oppdatering.deltakelsesprosent,
        bakgrunnsinformasjon = oppdatering.bakgrunnsinformasjon,
        deltakelsesinnhold = oppdatering.deltakelsesinnhold,
        status = oppdatering.status,
        historikk = oppdatering.historikk,
    )
}

const val FERIETILLEGG = 5L

fun years(n: Long): Duration = Duration.of(n * 365, ChronoUnit.DAYS)

fun months(n: Long): Duration = Duration.of(n * 30, ChronoUnit.DAYS)

fun weeks(n: Long): Duration = Duration.of(n * 7, ChronoUnit.DAYS)
