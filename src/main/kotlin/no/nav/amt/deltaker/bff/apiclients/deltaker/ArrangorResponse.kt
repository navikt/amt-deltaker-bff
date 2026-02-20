package no.nav.amt.deltaker.bff.apiclients.deltaker

import no.nav.amt.deltaker.bff.deltaker.model.ArrangorModel

data class ArrangorResponse(
    // Dette er navnet som skal brukes for alle praktiske formål
    // Men ikke nødvendigvis navnet til underenheten som svarer til orgnr
    val navn: String,
    val organisasjonsnummer: String, // Fjerne orgnr?
) {
    fun toArrangorModel() = ArrangorModel(navn)
}
