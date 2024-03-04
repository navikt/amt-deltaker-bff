package no.nav.amt.deltaker.bff.navansatt

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
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
        return oppdaterNavAnsatt(navAnsatt)
    }

    suspend fun hentEllerOpprettNavAnsatt(id: UUID): NavAnsatt {
        repository.get(id)?.let { return it }

        log.info("Fant ikke nav-ansatt med id $id, henter fra amt-person-service")
        val navAnsatt = amtPersonServiceClient.hentNavAnsatt(id)
        return oppdaterNavAnsatt(navAnsatt)
    }

    fun oppdaterNavAnsatt(navAnsatt: NavAnsatt): NavAnsatt {
        return repository.upsert(navAnsatt)
    }

    fun slettNavAnsatt(navAnsattId: UUID) {
        repository.delete(navAnsattId)
    }

    fun hentAnsatteForDeltaker(deltaker: Deltaker): Map<String, NavAnsatt> {
        val veilederIdenter = listOfNotNull(
            deltaker.vedtaksinformasjon?.opprettetAv,
            deltaker.vedtaksinformasjon?.sistEndretAv,
        ).distinct()

        return hentAnsatte(veilederIdenter)
    }

    fun hentAnsatteForHistorikk(historikk: List<DeltakerHistorikk>): Map<String, NavAnsatt> {
        val veilederIdenter = historikk.flatMap {
            when (it) {
                is DeltakerHistorikk.Endring -> {
                    listOf(it.endring.endretAv)
                }

                is DeltakerHistorikk.Vedtak -> {
                    listOfNotNull(
                        it.vedtak.sistEndretAv,
                        it.vedtak.opprettetAv,
                    )
                }
            }
        }.distinct()

        return hentAnsatte(veilederIdenter)
    }

    fun hentAnsatte(veilederIdenter: List<String>) = repository.getMany(veilederIdenter).associateBy { it.navIdent }
}
