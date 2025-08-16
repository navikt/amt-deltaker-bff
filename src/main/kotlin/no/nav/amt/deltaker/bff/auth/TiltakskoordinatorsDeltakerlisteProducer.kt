package no.nav.amt.deltaker.bff.auth

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

class TiltakskoordinatorsDeltakerlisteProducer(
    private val producer: Producer<String, String>,
) {
    fun produce(dto: TiltakskoordinatorsDeltakerlisteDto) {
        producer.produce(Environment.AMT_TILTAKSKOORDINATORS_DELTAKERLISTE_TOPIC, dto.id.toString(), objectMapper.writeValueAsString(dto))
    }

    fun produceTombstone(id: UUID) {
        producer.tombstone(Environment.AMT_TILTAKSKOORDINATORS_DELTAKERLISTE_TOPIC, id.toString())
    }
}
