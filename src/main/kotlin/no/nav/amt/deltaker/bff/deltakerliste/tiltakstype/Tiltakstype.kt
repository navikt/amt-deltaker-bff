package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode

fun DeltakerRegistreringInnhold.getInnholdselementerMedAnnet(tiltakstype: Tiltakskode): List<Innholdselement> {
    if (innholdselementer.isEmpty() && tiltakstype != Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET) return emptyList()
    return innholdselementer.plus(annetInnholdselement)
}

fun Innholdselement.toInnhold(valgt: Boolean = false, beskrivelse: String? = null) = Innhold(
    tekst = tekst,
    innholdskode = innholdskode,
    valgt = valgt,
    beskrivelse = beskrivelse,
)

val annetInnholdselement = Innholdselement("Annet", "annet")
