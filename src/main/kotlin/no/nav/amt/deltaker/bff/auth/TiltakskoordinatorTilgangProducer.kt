package no.nav.amt.deltaker.bff.auth

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.lib.kafka.Producer
import java.util.UUID

class TiltakskoordinatorTilgangProducer(
    private val producer: Producer<String, String>,
) {
    fun produce(dto: TiltakskoordinatorDeltakerlisteTilgangDto) {
        producer.produce(Environment.AMT_TILTAKSKOORDINATOR_TILGANG_TOPIC, dto.id.toString(), objectMapper.writeValueAsString(dto))
    }

    fun produceTombstone(id: UUID) {
        producer.tombstone(Environment.AMT_TILTAKSKOORDINATOR_TILGANG_TOPIC, id.toString())
    }
}

data class TiltakskoordinatorDeltakerlisteTilgangDto(
    val id: UUID,
    val navIdent: String,
    val gjennomforingId: UUID,
)

fun TiltakskoordinatorDeltakerlisteTilgang.toDto(navIdent: String) = TiltakskoordinatorDeltakerlisteTilgangDto(
    id = id,
    navIdent = navIdent,
    gjennomforingId = deltakerlisteId,
)
