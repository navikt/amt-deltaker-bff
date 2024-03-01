package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import java.util.UUID

data class OpprettKladdRequest(
    val deltakerlisteId: UUID,
    val personident: String,
)
