package no.nav.amt.deltaker.bff.auth

import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.TilgangType
import java.util.UUID

class TilgangskontrollService(
    private val poaoTilgangCachedClient: PoaoTilgangCachedClient,
) {
    fun verifiserSkrivetilgang(navAnsattAzureId: UUID, norskIdent: String) {
        val tilgang = poaoTilgangCachedClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsattAzureId,
                TilgangType.SKRIVE,
                norskIdent,
            ),
        ).getOrDefault(Decision.Deny("Ansatt har ikke skrivetilgang til bruker", ""))

        if (tilgang.isDeny) {
            throw AuthorizationException("Ansatt har ikke skrivetilgang til bruker")
        }
    }

    fun verifiserLesetilgang(navAnsattAzureId: UUID, norskIdent: String) {
        val tilgang = poaoTilgangCachedClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsattAzureId,
                TilgangType.LESE,
                norskIdent,
            ),
        ).getOrDefault(Decision.Deny("Ansatt har ikke lesetilgang til bruker", ""))

        if (tilgang.isDeny) {
            throw AuthorizationException("Ansatt har ikke lesetilgang til bruker")
        }
    }
}
