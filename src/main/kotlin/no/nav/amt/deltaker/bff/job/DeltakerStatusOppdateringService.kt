package no.nav.amt.deltaker.bff.job

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.harIkkeStartet
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerStatusOppdateringService(
    private val deltakerRepository: DeltakerRepository,
    private val deltakerProducer: DeltakerProducer,
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
    }

    private fun oppdaterStatusTilDeltar() {
        val deltakere = deltakerRepository.skalHaStatusDeltar()

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
    }

    private fun oppdaterDeltaker(deltaker: Deltaker) {
        deltakerRepository.upsert(deltaker)
        deltakerProducer.produce(deltaker)
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
