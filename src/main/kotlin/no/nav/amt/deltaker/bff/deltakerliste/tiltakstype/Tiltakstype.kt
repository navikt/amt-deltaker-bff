package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Innsatsgruppe
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
        UKJENT, // midlertidig kode til alle har blitt lest inn;
    }
}

data class DeltakerRegistreringInnhold(
    val innholdselementer: List<Innholdselement>,
    val ledetekst: String,
)

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
