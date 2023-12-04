package no.nav.amt.deltaker.bff.navansatt

import org.slf4j.LoggerFactory
import java.util.UUID

class NavAnsattService(
    private val repository: NavAnsattRepository,
    private val amtPersonServiceClient: AmtPersonServiceClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentEllerOpprettNavAnsatt(navIdent: String): NavAnsatt {
        repository.get(navIdent)?.let { return it }

        log.info("Fant ikke nav-ansatt med ident $navIdent, henter fra amt-person-service")
        val navAnsatt = amtPersonServiceClient.hentNavAnsatt(navIdent)
        oppdaterNavAnsatt(navAnsatt)
        return navAnsatt
    }

    fun oppdaterNavAnsatt(navAnsatt: NavAnsatt) {
        repository.upsert(navAnsatt)
    }

    fun slettNavAnsatt(navAnsattId: UUID) {
        repository.delete(navAnsattId)
    }
}
