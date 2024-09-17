package no.nav.amt.deltaker.bff.navansatt.navenhet

import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class NavEnhetService(
    private val repository: NavEnhetRepository,
    private val amtPersonServiceClient: AmtPersonServiceClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentOpprettEllerOppdaterNavEnhet(enhetsnummer: String): NavEnhet {
        repository
            .get(enhetsnummer)
            ?.takeIf { it.sistEndret.isAfter(LocalDateTime.now().minusMonths(1)) }
            ?.let { return it.toNavEnhet() }

        log.info("Fant ikke oppdatert nav-enhet med nummer $enhetsnummer, henter fra amt-person-service")
        val navEnhet = amtPersonServiceClient.hentNavEnhet(enhetsnummer)
        return repository.upsert(navEnhet).toNavEnhet()
    }

    fun hentEnhet(id: UUID) = repository.get(id)?.toNavEnhet()

    fun hentEnheterForHistorikk(historikk: List<DeltakerHistorikk>): Map<UUID, NavEnhet> {
        val ider = historikk.flatMap { it.navEnheter() }.distinct()
        return hentEnheter(ider)
    }

    private fun hentEnheter(enhetIder: List<UUID>) = repository.getMany(enhetIder).map { it.toNavEnhet() }.associateBy { it.id }
}
