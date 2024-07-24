package no.nav.amt.deltaker.bff.deltaker.forslag

import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingProducer
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.lib.models.arrangor.melding.Forslag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class ForslagService(
    private val forslagRepository: ForslagRepository,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
    private val arrangorMeldingProducer: ArrangorMeldingProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getForDeltaker(deltakerId: UUID) = forslagRepository.getForDeltaker(deltakerId)

    fun get(id: UUID) = forslagRepository.get(id)

    fun upsert(forslag: Forslag) = forslagRepository.upsert(forslag)

    fun delete(id: UUID) {
        forslagRepository.delete(id)
        log.info("Slettet godkjent forslag $id")
    }

    fun kanLagres(deltakerId: UUID) = forslagRepository.kanLagres(deltakerId)

    suspend fun avvisForslag(
        opprinneligForslag: Forslag,
        begrunnelse: String,
        avvistAvAnsatt: String,
        avvistAvEnhet: String,
    ) {
        val navAnsatt = navAnsattService.hentEllerOpprettNavAnsatt(avvistAvAnsatt)
        val navEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(avvistAvEnhet)
        val avvistForslag = opprinneligForslag.copy(
            status = Forslag.Status.Avvist(
                avvistAv = Forslag.NavAnsatt(
                    id = navAnsatt.id,
                    enhetId = navEnhet.id,
                ),
                avvist = LocalDateTime.now(),
                begrunnelseFraNav = begrunnelse,
            ),
        )
        arrangorMeldingProducer.produce(avvistForslag)
        delete(opprinneligForslag.id)
        log.info("Avvist forslag for deltaker ${opprinneligForslag.deltakerId}")
    }
}
