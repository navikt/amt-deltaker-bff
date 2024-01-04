package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val samtykkeRepository: DeltakerSamtykkeRepository,
    private val deltakerlisteRepository: DeltakerlisteRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettKladd(
        deltakerlisteId: UUID,
        personident: String,
        opprettetAv: String,
        opprettetAvEnhet: String?,
    ): Deltaker {
        val eksisterendeDeltaker = deltakerService.get(personident, deltakerlisteId).getOrNull()

        if (eksisterendeDeltaker != null && !eksisterendeDeltaker.harSluttet()) {
            log.warn("Deltakeren er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker
        }

        val deltakerliste = deltakerlisteRepository.get(deltakerlisteId).getOrThrow()
        val deltaker = nyKladd(personident, deltakerliste, opprettetAv, opprettetAvEnhet)

        deltakerService.upsert(deltaker)

        return deltakerService.get(deltaker.id).getOrThrow()
    }

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

    private fun nyKladd(
        personident: String,
        deltakerliste: Deltakerliste,
        opprettetAv: String,
        opprettetAvEnhet: String?,
    ): Deltaker =
        Deltaker(
            id = UUID.randomUUID(),
            personident = personident,
            deltakerliste = deltakerliste,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            bakgrunnsinformasjon = null,
            mal = emptyList(),
            status = nyDeltakerStatus(DeltakerStatus.Type.KLADD),
            sistEndretAv = opprettetAv,
            sistEndret = LocalDateTime.now(),
            sistEndretAvEnhet = opprettetAvEnhet,
            opprettet = LocalDateTime.now(),
        )
}
