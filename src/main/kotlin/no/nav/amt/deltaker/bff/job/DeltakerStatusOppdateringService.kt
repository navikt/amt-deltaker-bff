package no.nav.amt.deltaker.bff.job

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.harIkkeStartet
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerStatusOppdateringService(
    private val deltakerRepository: DeltakerRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun oppdaterDeltakerStatuser() {
        oppdaterTilAvsluttendeStatus()
        oppdaterStatusTilDeltar()
    }

    private fun oppdaterTilAvsluttendeStatus() {
        val deltakereSomSkalHaAvsluttendeStatus =
            deltakerRepository.skalHaAvsluttendeStatus().plus(deltakerRepository.deltarPaAvsluttetDeltakerliste())
                .distinct()

        val skalBliIkkeAktuell = deltakereSomSkalHaAvsluttendeStatus.filter { it.status.harIkkeStartet() }
        val skalBliAvbrutt = deltakereSomSkalHaAvsluttendeStatus
            .filter { it.status.type == DeltakerStatus.Type.DELTAR }
            .filter { sluttetForTidlig(it) }

        val skalBliHarSluttet = deltakereSomSkalHaAvsluttendeStatus
            .filter { it.status.type == DeltakerStatus.Type.DELTAR }
            .filter { !it.deltarPaKurs() }

        val skalBliFullfort = deltakereSomSkalHaAvsluttendeStatus
            .filter { it.status.type == DeltakerStatus.Type.DELTAR }
            .filter { it.deltarPaKurs() && !sluttetForTidlig(it) }

        skalBliIkkeAktuell.forEach {
            oppdaterDeltaker(
                it.copy(
                    status = DeltakerStatus(
                        id = UUID.randomUUID(),
                        type = DeltakerStatus.Type.IKKE_AKTUELL,
                        aarsak = null,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )
        }
        log.info("Endret status til IKKE AKTUELL for ${skalBliIkkeAktuell.size}")

        skalBliAvbrutt.forEach {
            oppdaterDeltaker(
                it.copy(
                    status = DeltakerStatus(
                        id = UUID.randomUUID(),
                        type = DeltakerStatus.Type.AVBRUTT,
                        aarsak = null,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )
        }
        log.info("Endret status til AVBRUTT for ${skalBliAvbrutt.size}")

        skalBliHarSluttet.forEach {
            oppdaterDeltaker(
                it.copy(
                    status = DeltakerStatus(
                        id = UUID.randomUUID(),
                        type = DeltakerStatus.Type.HAR_SLUTTET,
                        aarsak = null,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )
        }
        log.info("Endret status til HAR SLUTTET for ${skalBliHarSluttet.size}")

        skalBliFullfort.forEach {
            oppdaterDeltaker(
                it.copy(
                    status = DeltakerStatus(
                        id = UUID.randomUUID(),
                        type = DeltakerStatus.Type.FULLFORT,
                        aarsak = null,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )
        }
        log.info("Endret status til FULLFØRT for ${skalBliFullfort.size}")
    }

    private fun oppdaterStatusTilDeltar() {
        val deltakere = deltakerRepository.skalHaStatusDeltar().distinct()

        deltakere.forEach {
            oppdaterDeltaker(
                it.copy(
                    status = DeltakerStatus(
                        id = UUID.randomUUID(),
                        type = DeltakerStatus.Type.DELTAR,
                        aarsak = null,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )
        }
        log.info("Endret status til DELTAR for ${deltakere.size}")
    }

    private fun oppdaterDeltaker(deltaker: Deltaker) {
        deltakerRepository.upsert(deltaker)
        log.info("Oppdatert status for deltaker med id ${deltaker.id}")
    }

    private fun sluttetForTidlig(deltaker: Deltaker): Boolean {
        if (!deltaker.deltarPaKurs()) {
            return false
        }
        deltaker.deltakerliste.sluttDato?.let {
            return deltaker.sluttdato?.isBefore(it) == true
        }
        return false
    }
}
