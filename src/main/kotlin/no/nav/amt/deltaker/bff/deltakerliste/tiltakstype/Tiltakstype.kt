package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode

fun getInnholdselementer(innholdselementer: List<Innholdselement>?, tiltakstype: Tiltakskode): List<Innholdselement> {
    val skalKunHaAnnetInnhold = tiltakstype in listOf(
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
    )
    if (skalKunHaAnnetInnhold) return listOf(annetInnholdselement)

    if (innholdselementer == null || innholdselementer.isEmpty()) return emptyList()
    return innholdselementer.plus(annetInnholdselement)
}

fun Innholdselement.toInnhold(valgt: Boolean = false, beskrivelse: String? = null) = Innhold(
    tekst = tekst,
    innholdskode = innholdskode,
    valgt = valgt,
    beskrivelse = beskrivelse,
)

val annetInnholdselement = Innholdselement("Annet", "annet")
