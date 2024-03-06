package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import org.slf4j.LoggerFactory
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val navBrukerService: NavBrukerService,
    private val amtDeltakerClient: AmtDeltakerClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettKladd(
        deltakerlisteId: UUID,
        personident: String,
    ): Deltaker {
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

        deltakerService.oppdaterDeltaker(
            kladd.opprinneligDeltaker,
            kladd.opprinneligDeltaker.status,
            kladd.pamelding,
        )
    }

    suspend fun upsertUtkast(utkast: Utkast) {
        amtDeltakerClient.utkast(utkast)
    }

    suspend fun slettKladd(deltaker: Deltaker): Boolean {
        if (deltaker.status.type != DeltakerStatus.Type.KLADD) {
            log.warn("Kan ikke slette deltaker med id ${deltaker.id} som har status ${deltaker.status.type}")
            return false
        }
        amtDeltakerClient.slettKladd(deltaker.id)
        deltakerService.delete(deltaker.id)
        return true
    }

    suspend fun avbrytUtkast(deltakerId: UUID, avbruttAvEnhet: String, avbruttAv: String) {
        amtDeltakerClient.avbrytUtkast(deltakerId, avbruttAv, avbruttAvEnhet)
    }
}
