package no.nav.amt.deltaker.bff.auth

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.lib.kafka.Producer
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

data class TiltakskoordinatorsDeltakerlisteDto(
    val id: UUID,
    val navIdent: String,
    val gjennomforingId: UUID,
)

fun TiltakskoordinatorDeltakerlisteTilgang.toDto(navIdent: String) = TiltakskoordinatorsDeltakerlisteDto(
    id = id,
    navIdent = navIdent,
    gjennomforingId = deltakerlisteId,
)
