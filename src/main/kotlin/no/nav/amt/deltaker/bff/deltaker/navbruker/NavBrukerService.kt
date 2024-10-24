package no.nav.amt.deltaker.bff.deltaker.navbruker

import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import org.slf4j.LoggerFactory

class NavBrukerService(
    private val amtPersonServiceClient: AmtPersonServiceClient,
    private val repository: NavBrukerRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getOrCreate(personident: String): Result<NavBruker> {
        val brukerResult = repository.get(personident)
        if (brukerResult.isSuccess) return brukerResult

        val bruker = amtPersonServiceClient.hentNavBruker(personident)

        log.info("Oppretter nav-bruker ${bruker.personId}")
        return repository.upsert(bruker)
    }

    fun upsert(navBruker: NavBruker) {
        val bruker = repository.get(navBruker.personId).getOrNull()
        if (navBruker != bruker) repository.upsert(navBruker)
    }
}
