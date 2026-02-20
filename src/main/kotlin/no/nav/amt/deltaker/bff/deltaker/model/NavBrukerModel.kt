package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.person.Oppfolgingsperiode
import no.nav.amt.lib.models.person.address.Adresse
import no.nav.amt.lib.models.person.address.Adressebeskyttelse

data class NavBrukerModel(
    val personident: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val navVeileder: String?,
    val navEnhet: String?,
    val telefon: String?,
    val epost: String?,
    val erSkjermet: Boolean,
    val adresse: Adresse?,
    val adressebeskyttelse: Adressebeskyttelse?,
    val oppfolgingsperioder: List<Oppfolgingsperiode>,
    val innsatsgruppe: Innsatsgruppe?,
    val erDigital: Boolean,
) {
    val harAktivOppfolgingsperiode: Boolean
        get() = oppfolgingsperioder.any { it.erAktiv() }

    companion object {
        fun fromNavBrukerResponse(response: no.nav.amt.deltaker.bff.apiclients.deltaker.NavBrukerResponse): NavBrukerModel = NavBrukerModel(
            personident = response.personident,
            fornavn = response.fornavn,
            mellomnavn = response.mellomnavn,
            etternavn = response.etternavn,
            navVeileder = null, // Dette må hentes fra en annen kilde
            navEnhet = null, // Dette må hentes fra en annen kilde
            telefon = response.telefon,
            epost = response.epost,
            erSkjermet = response.erSkjermet,
            adresse = response.adresse,
            adressebeskyttelse = response.adressebeskyttelse,
            oppfolgingsperioder = response.oppfolgingsperioder,
            innsatsgruppe = response.innsatsgruppe,
            erDigital = response.erDigital,
        )
    }
}
