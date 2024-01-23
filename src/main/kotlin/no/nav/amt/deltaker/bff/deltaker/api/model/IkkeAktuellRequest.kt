package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring

data class IkkeAktuellRequest(
    val aarsak: DeltakerEndring.Aarsak,
)
