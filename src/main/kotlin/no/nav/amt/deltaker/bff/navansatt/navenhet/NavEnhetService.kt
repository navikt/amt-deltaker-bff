package no.nav.amt.deltaker.bff.navansatt.navenhet

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.lib.models.arrangor.melding.Forslag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class NavEnhetService(
    private val repository: NavEnhetRepository,
    private val amtPersonServiceClient: AmtPersonServiceClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentOpprettEllerOppdaterNavEnhet(enhetsnummer: String): NavEnhet {
        repository.get(enhetsnummer)
            ?.takeIf { it.sistEndret.isAfter(LocalDateTime.now().minusMonths(1)) }
            ?.let { return it.toNavEnhet() }

        log.info("Fant ikke oppdatert nav-enhet med nummer $enhetsnummer, henter fra amt-person-service")
        val navEnhet = amtPersonServiceClient.hentNavEnhet(enhetsnummer)
        return repository.upsert(navEnhet).toNavEnhet()
    }

    fun hentEnhet(id: UUID) = repository.get(id)?.toNavEnhet()

    fun hentEnheterForHistorikk(historikk: List<DeltakerHistorikk>): Map<UUID, NavEnhet> {
        val ider = historikk.flatMap {
            when (it) {
                is DeltakerHistorikk.Endring -> {
                    listOf(it.endring.endretAvEnhet)
                }

                is DeltakerHistorikk.Vedtak -> {
                    listOfNotNull(
                        it.vedtak.sistEndretAvEnhet,
                        it.vedtak.opprettetAvEnhet,
                    )
                }

                is DeltakerHistorikk.Forslag -> {
                    when (val status = it.forslag.status) {
                        is Forslag.Status.VenterPaSvar,
                        is Forslag.Status.Tilbakekalt,
                        -> emptyList()
                        is Forslag.Status.Avvist -> listOfNotNull(status.avvistAv.enhetId)
                        is Forslag.Status.Godkjent -> listOfNotNull(status.godkjentAv.enhetId)
                    }
                }
            }
        }.distinct()

        return hentEnheter(ider)
    }

    private fun hentEnheter(enhetIder: List<UUID>) = repository.getMany(enhetIder).map { it.toNavEnhet() }.associateBy { it.id }
}
