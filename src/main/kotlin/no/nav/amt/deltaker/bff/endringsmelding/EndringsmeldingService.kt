package no.nav.amt.deltaker.bff.endringsmelding

import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import org.slf4j.LoggerFactory
import java.util.UUID

class EndringsmeldingService(
    private val deltakerService: DeltakerService,
    private val navAnsattService: NavAnsattService,
    private val endringsmeldingRepository: EndringsmeldingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun upsert(endringsmelding: Endringsmelding) {
        deltakerService.get(endringsmelding.deltakerId).onSuccess {
            log.info("Upserter endringsmelding ${endringsmelding.id}")
            endringsmelding.utfortAvNavAnsattId?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }
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
