package no.nav.amt.deltaker.bff.endringsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.kafka.Consumer
import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class EndringsmeldingConsumer(
    private val endringsmeldingService: EndringsmeldingService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String?> {

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_ENDRINGSMELDING_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        if (value == null) {
            endringsmeldingService.delete(key)
        } else {
            endringsmeldingService.upsert(objectMapper.readValue(value))
        }
    }

    override fun run() = consumer.run()
}
