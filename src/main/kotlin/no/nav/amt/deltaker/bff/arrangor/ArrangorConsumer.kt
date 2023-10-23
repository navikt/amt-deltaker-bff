package no.nav.amt.deltaker.bff.arrangor

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("ArrangorConsumer")

fun consumeArrangor(id: UUID, arrangor: String?) {
    if (arrangor == null) {
        ArrangorRepository.delete(id)
        log.info("Slettet arrangør med id $id")
    } else {
        ArrangorRepository.upsert(objectMapper().readValue(arrangor))
        log.info("Lagret arrangør med id $id")
    }
}
