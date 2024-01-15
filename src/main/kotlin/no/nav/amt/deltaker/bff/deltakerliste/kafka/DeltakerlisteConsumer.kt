package no.nav.amt.deltaker.bff.deltakerliste.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class DeltakerlisteConsumer(
    private val repository: DeltakerlisteRepository,
    private val arrangorService: ArrangorService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val consumer = ManagedKafkaConsumer(
        topic = Environment.DELTAKERLISTE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = "amt-deltaker-bff-deltakerliste-consumer",
        ),
        consume = ::consumeDeltakerliste,
    )

    fun run() = consumer.run()

    suspend fun awaitReady() = consumer.awaitReady()

    suspend fun consumeDeltakerliste(id: UUID, deltakerliste: String?) {
        if (deltakerliste == null) {
            repository.delete(id)
        } else {
            handterDeltakerliste(objectMapper.readValue(deltakerliste))
        }
    }

    private suspend fun handterDeltakerliste(deltakerliste: DeltakerlisteDto) {
        if (!deltakerliste.tiltakstype.erStottet()) return

        val arrangor = arrangorService.hentArrangor(deltakerliste.virksomhetsnummer)
        repository.upsert(deltakerliste.toModel(arrangor))
    }
}
