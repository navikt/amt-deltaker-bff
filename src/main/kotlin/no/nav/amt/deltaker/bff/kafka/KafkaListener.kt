package no.nav.amt.deltaker.bff.kafka

import no.nav.amt.deltaker.bff.arrangor.consumeArrangor
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer

const val ARRANGOR_TOPIC = "amt.arrangor-v1"

fun configureKafkaListener(config: KafkaConfig) {
    val groupId = "amt-deltaker-bff-consumer-v1"

    val arrangorConsumer = ManagedKafkaConsumer(
        topic = ARRANGOR_TOPIC,
        config = config.consumerConfig(UUIDDeserializer(), StringDeserializer(), groupId),
        consume = ::consumeArrangor,
    )

    arrangorConsumer.run()
}
