package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.Deltakelsesmengder
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.toDeltakelsesmengder
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
) {
    val deltakelsesmengder: Deltakelsesmengder
        get() = startdato?.let { historikk.toDeltakelsesmengder().periode(it, sluttdato) } ?: historikk.toDeltakelsesmengder()


    val fattetVedtak
        get() = historikk
            .firstOrNull {
                it is DeltakerHistorikk.Vedtak && it.vedtak.gyldigTil == null && it.vedtak.fattet != null
            }?.let { (it as DeltakerHistorikk.Vedtak).vedtak }

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

    fun getDeltakerHistorikkSortert() = historikk.sortedByDescending { it.sistEndret }

    fun harSluttet(): Boolean = status.type in AVSLUTTENDE_STATUSER

    fun harSluttetForMindreEnnToMndSiden(): Boolean = harSluttet() && status.gyldigFra.toLocalDate().isAfter(LocalDate.now().minusMonths(2))

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
            Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> years(2)
            Tiltakstype.Tiltakskode.OPPFOLGING -> when (navBruker.innsatsgruppe) {
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                -> years(3)

                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Innsatsgruppe.STANDARD_INNSATS,
                null,
                -> null
            }

            Tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakstype.Tiltakskode.AVKLARING,
            -> weeks(12)

            Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> weeks(8)

            Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakstype.Tiltakskode.JOBBKLUBB,
            Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
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
            Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            -> years(3)

            Tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakstype.Tiltakskode.AVKLARING,
            -> weeks(12 + FERIETILLEGG)

            Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> weeks(8 + FERIETILLEGG)
            Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> years(4)

            Tiltakstype.Tiltakskode.OPPFOLGING -> when (navBruker.innsatsgruppe) {
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> years(1)
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                -> years(3).plus(months(6))

                else -> null
            }

            Tiltakstype.Tiltakskode.JOBBKLUBB,
            Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            -> null
        }
}

const val FERIETILLEGG = 4L

fun years(n: Long) = Duration.of(n * 365, ChronoUnit.DAYS)

fun months(n: Long) = Duration.of(n * 30, ChronoUnit.DAYS)

fun weeks(n: Long) = Duration.of(n * 7, ChronoUnit.DAYS)
