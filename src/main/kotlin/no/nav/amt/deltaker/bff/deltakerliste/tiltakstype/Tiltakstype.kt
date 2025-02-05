package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import no.nav.amt.deltaker.bff.deltaker.model.Innsatsgruppe
import no.nav.amt.lib.models.deltaker.Innhold
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenaKode: ArenaKode,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val innhold: DeltakerRegistreringInnhold?,
) {
    enum class ArenaKode {
        ARBFORB,
        ARBRRHDAG,
        AVKLARAG,
        DIGIOPPARB,
        INDOPPFAG,
        GRUFAGYRKE,
        GRUPPEAMO,
        JOBBK,
        VASV,
    }

    enum class Tiltakskode {
        ARBEIDSFORBEREDENDE_TRENING,
        ARBEIDSRETTET_REHABILITERING,
        AVKLARING,
        DIGITALT_OPPFOLGINGSTILTAK,
        GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        GRUPPE_FAG_OG_YRKESOPPLAERING,
        JOBBKLUBB,
        OPPFOLGING,
        VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        ;

        fun erKurs() = this in kursTiltak
    }

    companion object {
        val kursTiltak = setOf(
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.JOBBKLUBB,
        )
    }

    fun erKurs() = this.tiltakskode.erKurs()
}

data class DeltakerRegistreringInnhold(
    val innholdselementer: List<Innholdselement>,
    val ledetekst: String,
) {
    fun getInnholdselementerMedAnnet(tiltakstype: Tiltakstype.Tiltakskode): List<Innholdselement> {
        if (innholdselementer.isEmpty() && tiltakstype != Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET) return emptyList()
        return innholdselementer.plus(annetInnholdselement)
    }
}

data class Innholdselement(
    val tekst: String,
    val innholdskode: String,
) {
    fun toInnhold(valgt: Boolean = false, beskrivelse: String? = null) = Innhold(
        tekst = tekst,
        innholdskode = innholdskode,
        valgt = valgt,
        beskrivelse = beskrivelse,
    )
}

val annetInnholdselement = Innholdselement("Annet", "annet")
