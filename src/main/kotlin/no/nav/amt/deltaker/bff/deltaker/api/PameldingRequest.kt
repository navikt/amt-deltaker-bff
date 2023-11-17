package no.nav.amt.deltaker.bff.deltaker.api

import java.util.UUID

data class PameldingRequest(
    val deltakerlisteId: UUID,
    val personident: String,
)
