package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus

data class AvbrytUtkastRequest(
    val aarsak: DeltakerStatus.Aarsak,
)
