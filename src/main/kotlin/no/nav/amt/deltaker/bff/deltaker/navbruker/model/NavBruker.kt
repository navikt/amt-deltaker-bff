package no.nav.amt.deltaker.bff.deltaker.navbruker.model

import no.nav.amt.deltaker.bff.deltaker.model.Innsatsgruppe
import java.util.UUID

data class NavBruker(
    val personId: UUID,
    val personident: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val adressebeskyttelse: Adressebeskyttelse?,
    val oppfolgingsperioder: List<Oppfolgingsperiode> = emptyList(),
    val innsatsgruppe: Innsatsgruppe?,
    val adresse: Adresse?,
    val erSkjermet: Boolean = false,
) {
    val erAdressebeskyttet get() = adressebeskyttelse != null

    fun harAktivOppfolgingsperiode(): Boolean = oppfolgingsperioder.find { it.erAktiv() } != null
}

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
}
