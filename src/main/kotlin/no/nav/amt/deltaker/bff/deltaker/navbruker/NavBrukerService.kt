package no.nav.amt.deltaker.bff.deltaker.navbruker

class NavBrukerService(
    private val repository: NavBrukerRepository,
) {
    fun upsert(navBruker: NavBruker) {
        val bruker = repository.get(navBruker.personId).getOrNull()
        if (navBruker != bruker) repository.upsert(navBruker)
    }
}
