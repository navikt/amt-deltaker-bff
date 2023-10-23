package no.nav.amt.deltaker.bff.arrangor

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

fun consumeArrangor(id: UUID, arrangor: String?) {
    val log = LoggerFactory.getLogger("ArrangorConsumer")
    val repository = ArrangorRepository()

    if (arrangor == null) {
        repository.delete(id)
        log.info("Slettet arrangør med id $id")
    } else {
        repository.upsert(objectMapper().readValue(arrangor))
        log.info("Lagret arrangør med id $id")
    }
}
