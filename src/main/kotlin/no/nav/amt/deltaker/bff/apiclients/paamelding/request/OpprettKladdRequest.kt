package no.nav.amt.deltaker.bff.apiclients.paamelding.request

import java.util.UUID

data class OpprettKladdRequest(
    val deltakerlisteId: UUID,
    val personident: String,
)
