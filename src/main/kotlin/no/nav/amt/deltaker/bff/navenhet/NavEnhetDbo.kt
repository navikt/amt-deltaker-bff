package no.nav.amt.deltaker.bff.navenhet

import java.time.LocalDateTime
import java.util.UUID

data class NavEnhetDbo(
    val id: UUID,
    val enhetsnummer: String,
    val navn: String,
    val sistEndret: LocalDateTime,
) {
    fun toNavEnhet(): NavEnhet = NavEnhet(
        id = id,
        enhetsnummer = enhetsnummer,
        navn = navn,
    )
}
