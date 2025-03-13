package no.nav.amt.deltaker.bff.deltaker.navbruker.model

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

    fun harAktivOppfolgingsperiode(): Boolean = oppfolgingsperioder.find { it.erAktiv() } != null
}

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
}
