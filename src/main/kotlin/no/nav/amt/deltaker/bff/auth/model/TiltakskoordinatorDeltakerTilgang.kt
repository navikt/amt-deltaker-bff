package no.nav.amt.deltaker.bff.auth.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class TiltakskoordinatorDeltakerTilgang(
    val deltaker: Deltaker,
    val tilgang: Boolean,
)
