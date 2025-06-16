package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.slf4j.LoggerFactory
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val navBrukerService: NavBrukerService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val navEnhetService: NavEnhetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettKladd(deltakerlisteId: UUID, personident: String): Deltaker {
        val eksisterendeDeltaker = deltakerService
            .getDeltakelser(personident, deltakerlisteId)
            .firstOrNull { !it.harSluttet() }

        if (eksisterendeDeltaker != null) {
            log.warn("Deltakeren ${eksisterendeDeltaker.id} er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker
        }

        val kladd = amtDeltakerClient.opprettKladd(
            deltakerlisteId = deltakerlisteId,
            personident = personident,
        )

        navBrukerService.upsert(kladd.navBruker)
        val deltaker = deltakerService.opprettDeltaker(kladd).getOrThrow()

        MetricRegister.OPPRETTET_KLADD.inc()

        return deltaker
    }

    fun upsertKladd(kladd: Kladd) {
        require(kladd.opprinneligDeltaker.status.type == DeltakerStatus.Type.KLADD) {
            "Kan ikke upserte kladd for deltaker ${kladd.opprinneligDeltaker.id} " +
                "med status ${kladd.opprinneligDeltaker.status.type}," +
                "status må være ${DeltakerStatus.Type.KLADD}."
        }

        deltakerService.oppdaterKladd(
            kladd.opprinneligDeltaker,
            kladd.opprinneligDeltaker.status,
            kladd.pamelding,
        )
    }

    suspend fun upsertUtkast(utkast: Utkast): Deltaker {
        navEnhetService.hentOpprettEllerOppdaterNavEnhet(utkast.pamelding.endretAvEnhet)
        val deltakeroppdatering = amtDeltakerClient.utkast(utkast)

        deltakerService.oppdaterDeltaker(deltakeroppdatering)
        return deltakerService.get(utkast.deltakerId).getOrThrow()
    }

    suspend fun slettKladd(deltaker: Deltaker) = deltakerService.slettKladd(deltaker)

    suspend fun avbrytUtkast(
        deltakerId: UUID,
        avbruttAvEnhet: String,
        avbruttAv: String,
    ) {
        navEnhetService.hentOpprettEllerOppdaterNavEnhet(avbruttAvEnhet)
        amtDeltakerClient.avbrytUtkast(deltakerId, avbruttAv, avbruttAvEnhet)
    }

    fun getKladder(personident: String): List<Deltaker> {
        return deltakerService.getDeltakelser(personident).filter { it.status.type == DeltakerStatus.Type.KLADD }
    }

    fun getKladderForDeltakerliste(deltakerlisteId: UUID): List<Deltaker> {
        return deltakerService.getKladderForDeltakerliste(deltakerlisteId)
    }
}
