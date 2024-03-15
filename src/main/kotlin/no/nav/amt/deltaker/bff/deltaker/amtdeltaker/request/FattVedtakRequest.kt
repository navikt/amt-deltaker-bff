package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import java.util.UUID

data class FattVedtakRequest(
    val id: UUID,
    val fattetAvNav: Boolean,
    val sistEndretAv: UUID,
    val sistEndretAvEnhet: UUID,
)
