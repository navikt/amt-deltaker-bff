package no.nav.amt.deltaker.bff.deltaker.api

import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import java.util.UUID

data class PameldingResponse(
    val deltakerId: UUID,
    val deltakerliste: DeltakerlisteDTO,
)

data class DeltakerlisteDTO(
    val deltakerlisteId: UUID,
    val deltakerlisteNavn: String,
    val tiltakstype: Tiltak.Type,
    val arrangorNavn: String,
    val oppstartstype: Deltakerliste.Oppstartstype,
    val mal: List<Mal>,
)
