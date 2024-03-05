package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.db.VedtakRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.FattetAvNav
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltaker.model.VedtakDbo
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val vedtakRepository: VedtakRepository,
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

    fun upsertUtkast(utkast: Utkast) {
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

        val vedtak = vedtakRepository.getIkkeFattet(deltaker.id)

        vedtakRepository.upsert(oppdatertVedtak(vedtak, utkast, deltaker))

        MetricRegister.DELT_UTKAST.inc()
    }

    fun meldPaUtenGodkjenning(
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

        val vedtak = vedtakRepository.getIkkeFattet(deltaker.id)

        vedtakRepository.upsert(oppdatertVedtak(vedtak, utkast, deltaker, LocalDateTime.now()))

        MetricRegister.PAMELDT_UTEN_UTKAST.inc()
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

    private fun kanGodkjennes(utkast: Utkast) =
        utkast.opprinneligDeltaker.status.type in listOf(
            DeltakerStatus.Type.KLADD,
            DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
        )

    private fun oppdatertVedtak(
        original: Vedtak?,
        utkast: Utkast,
        deltaker: Deltaker,
        fattet: LocalDateTime? = null,
    ) = VedtakDbo(
        id = original?.id ?: UUID.randomUUID(),
        deltakerId = deltaker.id,
        fattet = fattet,
        gyldigTil = null,
        deltakerVedVedtak = deltaker,
        fattetAvNav = utkast.godkjentAvNav?.let { FattetAvNav(it.godkjentAv, it.godkjentAvEnhet) },
        opprettetAv = original?.opprettetAv ?: utkast.pamelding.endretAv,
        opprettetAvEnhet = original?.opprettetAvEnhet ?: utkast.pamelding.endretAvEnhet,
        opprettet = original?.opprettet ?: LocalDateTime.now(),
        sistEndretAv = utkast.pamelding.endretAv,
        sistEndretAvEnhet = utkast.pamelding.endretAvEnhet,
        sistEndret = LocalDateTime.now(),
    )

    fun avbrytUtkast(
        opprinneligDeltaker: Deltaker,
        endretAvEnhet: String?,
        navIdent: String,
    ) {
        if (opprinneligDeltaker.status.type != DeltakerStatus.Type.UTKAST_TIL_PAMELDING) {
            log.warn("Kan ikke avbryte utkast for deltaker med id ${opprinneligDeltaker.id} som har status ${opprinneligDeltaker.status.type}")
            throw IllegalArgumentException("Kan ikke avbryte utkast for deltaker med id ${opprinneligDeltaker.id} som har status ${opprinneligDeltaker.status.type}")
        }

        val status = nyDeltakerStatus(DeltakerStatus.Type.AVBRUTT_UTKAST)
        val deltaker = deltakerService.oppdaterDeltaker(opprinneligDeltaker, status)

        deltakerService.upsert(deltaker)
    }
}
