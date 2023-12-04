package no.nav.amt.deltaker.bff.navansatt

import java.util.UUID

data class NavAnsatt(
    val id: UUID,
    val navident: String,
    val navn: String,
)
