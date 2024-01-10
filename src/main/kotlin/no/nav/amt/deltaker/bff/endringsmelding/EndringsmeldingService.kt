package no.nav.amt.deltaker.bff.endringsmelding

import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import org.slf4j.LoggerFactory
import java.util.UUID

class EndringsmeldingService(
    private val deltakerService: DeltakerService,
    private val endringsmeldingRepository: EndringsmeldingRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun upsert(endringsmelding: Endringsmelding) {
        deltakerService.get(endringsmelding.deltakerId).onSuccess {
            log.info("Upserter endringsmelding ${endringsmelding.id}")
            endringsmeldingRepository.upsert(endringsmelding)
        }.onFailure {
            log.info(
                "Upserter ikke endringsmelding ${endringsmelding.id} " +
                    "for deltaker ${endringsmelding.deltakerId} som ikke finnes",
            )
        }
    }

    fun delete(id: UUID) {
        log.info("Sletter endringsmelding $id")
        endringsmeldingRepository.delete(id)
    }

    fun get(id: UUID) = endringsmeldingRepository.get(id)
}
