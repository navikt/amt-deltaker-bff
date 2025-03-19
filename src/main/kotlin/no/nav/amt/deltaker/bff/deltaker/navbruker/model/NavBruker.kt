package no.nav.amt.deltaker.bff.deltaker.navbruker.model

import no.nav.amt.deltaker.bff.tiltakskoordinator.DeltakerResponseUtils.Companion.ADRESSEBESKYTTET_PLACEHOLDER_NAVN
import no.nav.amt.deltaker.bff.tiltakskoordinator.DeltakerResponseUtils.Companion.SKJERMET_PERSON_PLACEHOLDER_NAVN
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Beskyttelsesmarkering
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
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
    val erSkjermet: Boolean,
    val navEnhetId: UUID?,
    val navVeilederId: UUID?,
) {
    val erAdressebeskyttet get() = adressebeskyttelse != null

    fun getBeskyttelsesmarkeringer(): List<Beskyttelsesmarkering> {
        val adressebeskyttelse =
            when (adressebeskyttelse) {
                null -> null
                Adressebeskyttelse.FORTROLIG -> Beskyttelsesmarkering.FORTROLIG
                Adressebeskyttelse.STRENGT_FORTROLIG -> Beskyttelsesmarkering.STRENGT_FORTROLIG
                Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND -> Beskyttelsesmarkering.STRENGT_FORTROLIG_UTLAND
            }

        val skjermet = if (erSkjermet) Beskyttelsesmarkering.SKJERMET else null

        return listOfNotNull(adressebeskyttelse, skjermet)
    }

    fun getVisningsnavn(tilgangTilBruker: Boolean): Triple<String, String?, String> {
        if (erAdressebeskyttet && !tilgangTilBruker) {
            return Triple(ADRESSEBESKYTTET_PLACEHOLDER_NAVN, null, "")
        }
        if (erSkjermet && !tilgangTilBruker) {
            return Triple(SKJERMET_PERSON_PLACEHOLDER_NAVN, null, "")
        }

        return Triple(fornavn, mellomnavn, etternavn)
    }

    fun harAktivOppfolgingsperiode(): Boolean = oppfolgingsperioder.find { it.erAktiv() } != null
}

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
}
