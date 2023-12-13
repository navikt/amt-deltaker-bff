package no.nav.amt.deltaker.bff.auth

import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.TilgangType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilgangskontrollService(
    private val poaoTilgangCachedClient: PoaoTilgangCachedClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun verifiserSkrivetilgang(navAnsattAzureId: UUID, norskIdent: String) {
        log.info("Sjekker tilgang for ansattid $navAnsattAzureId")
        val tilgang = poaoTilgangCachedClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsattAzureId,
                TilgangType.SKRIVE,
                norskIdent,
            ),
        ).getOrDefault(Decision.Deny("Ansatt har ikke skrivetilgang til bruker", ""))

        if (tilgang.isDeny) {
            log.error("Ikke skrivetilgang")
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
