package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerlisteDTO
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerHistorikkRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val deltakerlisteRepository: DeltakerlisteRepository,
    private val samtykkeRepository: DeltakerSamtykkeRepository,
    private val historikkRepository: DeltakerHistorikkRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun opprettDeltaker(
        deltakerlisteId: UUID,
        personident: String,
        opprettetAv: String,
    ): DeltakerResponse {
        val deltakerliste = deltakerlisteRepository.get(deltakerlisteId)
            ?: throw NoSuchElementException("Fant ikke deltakerliste med id $deltakerlisteId")
        val eksisterendeDeltaker = deltakerRepository.get(personident, deltakerlisteId)
        if (eksisterendeDeltaker != null && !eksisterendeDeltaker.harSluttet()) {
            log.warn("Deltakeren er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker.toDeltakerResponse(deltakerliste)
        }
        val deltaker = nyttUtkast(personident, deltakerlisteId, opprettetAv)
        log.info("Oppretter deltaker med id ${deltaker.id}")
        deltakerRepository.upsert(deltaker)
        return deltakerRepository.get(deltaker.id)?.toDeltakerResponse(deltakerliste)
            ?: throw RuntimeException("Kunne ikke hente opprettet deltaker med id ${deltaker.id}")
    }

    fun get(id: UUID) = deltakerRepository.get(id) ?: throw NoSuchElementException("Fant ikke deltaker med id: $id")

    fun opprettForslag(opprinneligDeltaker: Deltaker, forslag: OppdatertDeltaker, endretAv: String) {
        val status = if (opprinneligDeltaker.status.type == DeltakerStatus.Type.UTKAST) {
            nyDeltakerStatus(DeltakerStatus.Type.FORSLAG_TIL_INNBYGGER)
        } else {
            opprinneligDeltaker.status
        }

        val deltaker = opprinneligDeltaker.copy(
            mal = forslag.mal,
            bakgrunnsinformasjon = forslag.bakgrunnsinformasjon,
            deltakelsesprosent = forslag.deltakelsesprosent,
            dagerPerUke = forslag.dagerPerUke,
            status = status,
            sistEndretAv = endretAv,
            sistEndret = LocalDateTime.now(),
        )

        deltakerRepository.upsert(deltaker)

        val samtykkeId = samtykkeRepository.getIkkeGodkjent(deltaker.id)?.id ?: UUID.randomUUID()

        samtykkeRepository.upsert(
            DeltakerSamtykke(
                id = samtykkeId,
                deltakerId = deltaker.id,
                godkjent = null,
                gyldigTil = null,
                deltakerVedSamtykke = deltaker,
                godkjentAvNav = forslag.godkjentAvNav,
            ),
        )
    }

    fun meldPaUtenGodkjenning(opprinneligDeltaker: Deltaker, oppdatertDeltaker: OppdatertDeltaker, endretAv: String) {
        if (oppdatertDeltaker.godkjentAvNav == null) {
            log.error("Kan ikke forhåndsgodkjenne deltaker med id ${opprinneligDeltaker.id} uten begrunnelse, skal ikke kunne skje!")
            throw RuntimeException("Kan ikke forhåndsgodkjenne deltaker uten begrunnelse")
        }
        val deltaker = opprinneligDeltaker.copy(
            mal = oppdatertDeltaker.mal,
            bakgrunnsinformasjon = oppdatertDeltaker.bakgrunnsinformasjon,
            deltakelsesprosent = oppdatertDeltaker.deltakelsesprosent,
            dagerPerUke = oppdatertDeltaker.dagerPerUke,
            status = nyDeltakerStatus(DeltakerStatus.Type.VENTER_PA_OPPSTART), // her skal vi mest sannsynlig ha en annen status, men det er ikke avklart hva den skal være
            sistEndretAv = endretAv,
            sistEndret = LocalDateTime.now(),
        )

        deltakerRepository.upsert(deltaker)

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

    fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        endringType: DeltakerEndringType,
        endring: DeltakerEndring,
        endretAv: String,
    ): DeltakerResponse {
        if (opprinneligDeltaker.harSluttet()) {
            log.warn("Kan ikke endre på deltaker med id ${opprinneligDeltaker.id}, deltaker har sluttet")
            throw IllegalArgumentException("Kan ikke endre deltaker som har sluttet")
        }
        val deltaker = when (endring) {
            is DeltakerEndring.EndreBakgrunnsinformasjon -> opprinneligDeltaker.copy(
                bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
                sistEndretAv = endretAv,
                sistEndret = LocalDateTime.now(),
            )
            is DeltakerEndring.EndreMal -> opprinneligDeltaker.copy(
                mal = endring.mal,
                sistEndretAv = endretAv,
                sistEndret = LocalDateTime.now(),
            )
            is DeltakerEndring.EndreDeltakelsesmengde -> opprinneligDeltaker.copy(
                deltakelsesprosent = endring.deltakelsesprosent,
                dagerPerUke = endring.dagerPerUke,
                sistEndretAv = endretAv,
                sistEndret = LocalDateTime.now(),
            )
            is DeltakerEndring.EndreStartdato -> opprinneligDeltaker.copy(
                startdato = endring.startdato,
                sistEndretAv = endretAv,
                sistEndret = LocalDateTime.now(),
            )
            is DeltakerEndring.EndreSluttdato -> opprinneligDeltaker.copy(
                sluttdato = endring.sluttdato,
                sistEndretAv = endretAv,
                sistEndret = LocalDateTime.now(),
            )
        }

        deltakerRepository.upsert(deltaker)
        historikkRepository.upsert(
            DeltakerHistorikk(
                id = UUID.randomUUID(),
                deltakerId = deltaker.id,
                endringType = endringType,
                endring = endring,
                endretAv = endretAv,
                endret = LocalDateTime.now(),
            ),
        )
        val deltakerliste = deltakerlisteRepository.get(deltaker.deltakerlisteId)
            ?: throw NoSuchElementException("Fant ikke deltakerliste med id ${deltaker.deltakerlisteId}")
        return deltakerRepository.get(deltaker.id)?.toDeltakerResponse(deltakerliste)
            ?: throw RuntimeException("Kunne ikke hente finne deltaker med id ${deltaker.id}")
    }

    fun slettUtkast(deltakerId: UUID) {
        deltakerRepository.slettUtkast(deltakerId)
    }

    private fun nyttUtkast(personident: String, deltakerlisteId: UUID, opprettetAv: String): Deltaker =
        Deltaker(
            id = UUID.randomUUID(),
            personident = personident,
            deltakerlisteId = deltakerlisteId,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            bakgrunnsinformasjon = null,
            mal = emptyList(),
            status = nyDeltakerStatus(DeltakerStatus.Type.UTKAST),
            sistEndretAv = opprettetAv,
            sistEndret = LocalDateTime.now(),
            opprettet = LocalDateTime.now(),
        )
}

private fun nyDeltakerStatus(type: DeltakerStatus.Type, aarsak: DeltakerStatus.Aarsak? = null) = DeltakerStatus(
    id = UUID.randomUUID(),
    type = type,
    aarsak = aarsak,
    gyldigFra = LocalDateTime.now(),
    gyldigTil = null,
    opprettet = LocalDateTime.now(),
)

private fun Deltaker.toDeltakerResponse(deltakerliste: Deltakerliste): DeltakerResponse {
    return DeltakerResponse(
        deltakerId = id,
        deltakerliste = DeltakerlisteDTO(
            deltakerlisteId = deltakerlisteId,
            deltakerlisteNavn = deltakerliste.navn,
            tiltakstype = deltakerliste.tiltak.type,
            arrangorNavn = deltakerliste.arrangor.navn,
            oppstartstype = deltakerliste.getOppstartstype(),
        ),
        status = status,
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        mal = mal,
    )
}
