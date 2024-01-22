package no.nav.amt.deltaker.bff.deltaker.navbruker

import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import org.slf4j.LoggerFactory

class NavBrukerService(
    private val repository: NavBrukerRepository,
    private val amtPersonServiceClient: AmtPersonServiceClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    suspend fun get(personident: String): Result<NavBruker> {
        val brukerResult = repository.get(personident)
        if (brukerResult.isSuccess) return brukerResult

        val bruker = amtPersonServiceClient.hentNavBruker(personident)

        log.info("Oppretter nav-bruker ${bruker.personId}")
        return repository.upsert(bruker)
    }
}
