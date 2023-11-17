package no.nav.amt.deltaker.bff.deltakerliste

import no.nav.amt.deltaker.bff.arrangor.Arrangor

data class DeltakerlisteMedArrangor(
    val deltakerliste: Deltakerliste,
    val arrangor: Arrangor,
)
