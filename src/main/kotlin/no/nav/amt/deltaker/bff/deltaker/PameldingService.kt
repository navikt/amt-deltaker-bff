package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val samtykkeRepository: DeltakerSamtykkeRepository,
    private val deltakerlisteRepository: DeltakerlisteRepository,
    private val navBrukerService: NavBrukerService,
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
        val navBruker = navBrukerService.get(personident).getOrThrow()
        val deltaker = nyKladd(navBruker, deltakerliste, opprettetAv, opprettetAvEnhet)

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

        val samtykke = samtykkeRepository.getIkkeGodkjent(deltaker.id)

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykke?.id ?: UUID.randomUUID(),
                deltakerId = deltaker.id,
                godkjent = null,
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = utkast.godkjentAvNav,
                opprettetAv = samtykke?.opprettetAv ?: utkast.endretAv,
                opprettetAvEnhet = samtykke?.opprettetAvEnhet ?: utkast.endretAvEnhet,
                opprettet = samtykke?.opprettet ?: LocalDateTime.now(),
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

        val samtykke = samtykkeRepository.getIkkeGodkjent(deltaker.id)

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykke?.id ?: UUID.randomUUID(),
                deltakerId = deltaker.id,
                godkjent = LocalDateTime.now(),
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = oppdatertDeltaker.godkjentAvNav,
                opprettetAv = samtykke?.opprettetAv ?: oppdatertDeltaker.endretAv,
                opprettetAvEnhet = samtykke?.opprettetAvEnhet ?: oppdatertDeltaker.endretAvEnhet,
                opprettet = samtykke?.opprettet ?: LocalDateTime.now(),
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
        navBruker: NavBruker,
        deltakerliste: Deltakerliste,
        opprettetAv: String,
        opprettetAvEnhet: String?,
    ): Deltaker =
        Deltaker(
            id = UUID.randomUUID(),
            navBruker = navBruker,
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

    suspend fun avbrytUtkast(
        opprinneligDeltaker: Deltaker,
        enhetsnummer: String?,
        navIdent: String,
        aarsak: DeltakerStatus.Aarsak
    ) {
        if (opprinneligDeltaker.status.type != DeltakerStatus.Type.UTKAST_TIL_PAMELDING) {
            log.warn("Kan ikke avbryte utkast for deltaker med id ${opprinneligDeltaker.id} som har status ${opprinneligDeltaker.status.type}")
            throw IllegalArgumentException("Kan ikke avbryte utkast for deltaker med id ${opprinneligDeltaker.id} som har status ${opprinneligDeltaker.status.type}")
        }
        val status = nyDeltakerStatus(DeltakerStatus.Type.AVBRUTT_UTKAST, aarsak)
        val deltaker = deltakerService.oppdaterDeltaker(opprinneligDeltaker, status, enhetsnummer, navIdent)

        deltakerService.upsert(deltaker)
    }
}
