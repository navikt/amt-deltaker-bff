package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val type: Tiltak.Type,
    val innhold: DeltakerRegistreringInnhold?,
)

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
