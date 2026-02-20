package no.nav.amt.deltaker.bff.apiclients.deltaker

data class ArrangorResponse(
    // Dette er navnet som skal brukes for alle praktiske formål
    // Men ikke nødvendigvis navnet til underenheten som svarer til orgnr
    val navn: String,
    val organisasjonsnummer: String, // Fjerne orgnr?
)
