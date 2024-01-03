package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val samtykkeRepository: DeltakerSamtykkeRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettUtkast(
        opprinneligDeltaker: Deltaker,
        utkast: OppdatertDeltaker,
    ) {
        val status = if (opprinneligDeltaker.status.type == DeltakerStatus.Type.KLADD) {
            nyDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING)
        } else {
            opprinneligDeltaker.status
        }

        val deltaker = deltakerService.oppdaterDeltaker(opprinneligDeltaker, status, utkast)

        val samtykkeId = samtykkeRepository.getIkkeGodkjent(deltaker.id)?.id ?: UUID.randomUUID()

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykkeId,
                deltakerId = deltaker.id,
                godkjent = null,
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = utkast.godkjentAvNav,
            ),
        )
    }

    suspend fun meldPaUtenGodkjenning(
        opprinneligDeltaker: Deltaker,
        oppdatertDeltaker: OppdatertDeltaker,
    ) {
        if (oppdatertDeltaker.godkjentAvNav == null) {
            log.error("Kan ikke forhåndsgodkjenne deltaker med id ${opprinneligDeltaker.id} uten begrunnelse")
            error("Kan ikke forhåndsgodkjenne deltaker uten begrunnelse")
        }

        val deltaker = deltakerService.oppdaterDeltaker(
            opprinneligDeltaker,
            // her skal vi mest sannsynlig ha en annen status, men det er ikke avklart hva den skal være
            nyDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            oppdatertDeltaker,
        )

        val samtykkeId = samtykkeRepository.getIkkeGodkjent(deltaker.id)?.id ?: UUID.randomUUID()

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykkeId,
                deltakerId = deltaker.id,
                godkjent = LocalDateTime.now(),
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = oppdatertDeltaker.godkjentAvNav,
            ),
        )
    }

    fun slettKladd(deltaker: Deltaker): Boolean {
        if (deltaker.status.type != DeltakerStatus.Type.KLADD) {
            log.warn("Kan ikke slette deltaker med id ${deltaker.id} som har status ${deltaker.status.type}")
            return false
        }
        deltakerService.delete(deltaker.id)
        return true
    }
}
