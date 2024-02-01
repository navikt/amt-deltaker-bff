package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.application.metrics.OPPRETTET_KLADD
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
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
        val eksisterendeDeltaker = deltakerService
            .getDeltakelser(personident, deltakerlisteId)
            .firstOrNull { !it.harSluttet() }

        if (eksisterendeDeltaker != null) {
            log.warn("Deltakeren ${eksisterendeDeltaker.id} er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker
        }

        val deltakerliste = deltakerlisteRepository.get(deltakerlisteId).getOrThrow()
        val navBruker = navBrukerService.get(personident).getOrThrow()
        val deltaker = nyDeltakerKladd(navBruker, deltakerliste, opprettetAv, opprettetAvEnhet)

        deltakerService.upsert(deltaker)

        OPPRETTET_KLADD.inc()

        return deltakerService.get(deltaker.id).getOrThrow()
    }

    suspend fun upsertKladd(kladd: Kladd) {
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
        val status = when (utkast.opprinneligDeltaker.status.type) {
            DeltakerStatus.Type.KLADD -> nyDeltakerStatus(DeltakerStatus.Type.UTKAST_TIL_PAMELDING)
            DeltakerStatus.Type.UTKAST_TIL_PAMELDING -> utkast.opprinneligDeltaker.status
            else -> throw IllegalArgumentException(
                "Kan ikke upserte ukast for deltaker ${utkast.opprinneligDeltaker.id} " +
                    "med status ${utkast.opprinneligDeltaker.status.type}," +
                    "status må være ${DeltakerStatus.Type.KLADD} eller ${DeltakerStatus.Type.UTKAST_TIL_PAMELDING}.",
            )
        }

        val deltaker = deltakerService.oppdaterDeltaker(utkast.opprinneligDeltaker, status, utkast.pamelding)

        val samtykke = samtykkeRepository.getIkkeGodkjent(deltaker.id)

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykke?.id ?: UUID.randomUUID(),
                deltakerId = deltaker.id,
                godkjent = null,
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = utkast.godkjentAvNav,
                opprettetAv = samtykke?.opprettetAv ?: utkast.pamelding.endretAv,
                opprettetAvEnhet = samtykke?.opprettetAvEnhet ?: utkast.pamelding.endretAvEnhet,
                opprettet = samtykke?.opprettet ?: LocalDateTime.now(),
            ),
        )
    }

    suspend fun meldPaUtenGodkjenning(
        utkast: Utkast,
    ) {
        require(utkast.godkjentAvNav != null) {
            "Kan ikke forhåndsgodkjenne deltaker med id ${utkast.opprinneligDeltaker.id} uten begrunnelse"
        }
        require(kanGodkjennes(utkast)) {
            "Kan ikke melde på uten godkjenning for deltaker ${utkast.opprinneligDeltaker.id}" +
                "med status ${utkast.opprinneligDeltaker.status.type}," +
                "status må være ${DeltakerStatus.Type.KLADD} eller ${DeltakerStatus.Type.UTKAST_TIL_PAMELDING}."
        }

        val deltaker = deltakerService.oppdaterDeltaker(
            utkast.opprinneligDeltaker,
            // her skal vi mest sannsynlig ha en annen status, men det er ikke avklart hva den skal være
            nyDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART),
            utkast.pamelding,
        )

        val samtykke = samtykkeRepository.getIkkeGodkjent(deltaker.id)

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykke?.id ?: UUID.randomUUID(),
                deltakerId = deltaker.id,
                godkjent = LocalDateTime.now(),
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = utkast.godkjentAvNav,
                opprettetAv = samtykke?.opprettetAv ?: utkast.pamelding.endretAv,
                opprettetAvEnhet = samtykke?.opprettetAvEnhet ?: utkast.pamelding.endretAvEnhet,
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

    private fun kanGodkjennes(utkast: Utkast) =
        utkast.opprinneligDeltaker.status.type in listOf(
            DeltakerStatus.Type.KLADD,
            DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
        )

    private fun nyDeltakerKladd(
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
}
