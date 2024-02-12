package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

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
)
