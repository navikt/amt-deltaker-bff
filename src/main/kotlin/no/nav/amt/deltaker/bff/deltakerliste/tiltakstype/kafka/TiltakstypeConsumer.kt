package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.deltakerliste.tiltakstype.kafka.TiltakstypeDto
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class TiltakstypeConsumer(
    private val repository: TiltakstypeRepository,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(autoOffsetReset = "earliest"),
) : Consumer<UUID, String?> {
    private val consumer = ManagedKafkaConsumer(
        topic = Environment.TILTAKSTYPE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID + "tiltakstyper",
        ),
        consume = ::consume,
    )

    override fun start() = consumer.start()

    override suspend fun close() = consumer.close()

    override suspend fun consume(key: UUID, value: String?) {
        value?.let { handterTiltakstype(objectMapper.readValue(it)) }
    }

    private fun handterTiltakstype(tiltakstype: TiltakstypeDto) {
        repository.upsert(tiltakstype.toModel())
    }
}
