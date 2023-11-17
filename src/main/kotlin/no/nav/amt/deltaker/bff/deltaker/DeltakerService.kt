package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.api.DeltakerlisteDTO
import no.nav.amt.deltaker.bff.deltaker.api.PameldingResponse
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteMedArrangor
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val deltakerlisteRepository: DeltakerlisteRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun opprettDeltaker(
        deltakerlisteId: UUID,
        personident: String,
        opprettetAv: String,
    ): PameldingResponse {
        val deltakerliste = deltakerlisteRepository.getDeltakerlisteMedArrangor(deltakerlisteId) ?: throw NoSuchElementException("Fant ikke deltakerliste med id $deltakerlisteId")
        val eksisterendeDeltaker = deltakerRepository.get(personident, deltakerlisteId)
        if (eksisterendeDeltaker != null && !eksisterendeDeltaker.harSluttet()) {
            log.warn("Deltakeren er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker.toPameldingResponse(deltakerliste)
        }
        val deltaker = getDeltaker(personident, deltakerlisteId, opprettetAv)
        log.info("Oppretter deltaker med id ${deltaker.id}")
        deltakerRepository.upsert(deltaker)
        return deltakerRepository.get(deltaker.id)?.toPameldingResponse(deltakerliste) ?: throw RuntimeException("Kunne ikke hente opprettet deltaker med id ${deltaker.id}")
    }

    private fun getDeltaker(personident: String, deltakerlisteId: UUID, opprettetAv: String): Deltaker =
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
            status = DeltakerStatus(
                id = UUID.randomUUID(),
                DeltakerStatus.Type.UTKAST,
                aarsak = null,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                opprettet = LocalDateTime.now(),
            ),
            sistEndretAv = opprettetAv,
            sistEndret = LocalDateTime.now(),
            opprettet = LocalDateTime.now(),
        )
}

private fun Deltaker.toPameldingResponse(deltakerlisteMedArrangor: DeltakerlisteMedArrangor): PameldingResponse {
    return PameldingResponse(
        deltakerId = id,
        deltakerliste = DeltakerlisteDTO(
            deltakerlisteId = deltakerlisteId,
            deltakerlisteNavn = deltakerlisteMedArrangor.deltakerliste.navn,
            tiltakstype = deltakerlisteMedArrangor.deltakerliste.tiltak.type,
            arrangorNavn = deltakerlisteMedArrangor.arrangor.navn,
            oppstartstype = deltakerlisteMedArrangor.deltakerliste.getOppstartstype(),
            mal = mal,
        ),
    )
}
