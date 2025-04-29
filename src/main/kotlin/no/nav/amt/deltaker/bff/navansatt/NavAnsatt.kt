package no.nav.amt.deltaker.bff.navansatt

import java.util.UUID

data class NavAnsatt(
    val id: UUID,
    val navIdent: String,
    val navn: String,
    val epost: String?,
    val telefon: String?,
    val navEnhetId: UUID?,
)
