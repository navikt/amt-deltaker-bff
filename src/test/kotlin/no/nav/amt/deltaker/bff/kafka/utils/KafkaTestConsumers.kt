package no.nav.amt.deltaker.bff.kafka.utils

import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer

private var id = 1

fun stringStringConsumer(topic: String, block: suspend (k: String, v: String) -> Unit): ManagedKafkaConsumer<String, String> {
    val config = LocalKafkaConfig(SingletonKafkaProvider.getHost()).consumerConfig(
        keyDeserializer = StringDeserializer(),
        valueDeserializer = StringDeserializer(),
        groupId = "test-consumer-${id++}",
    )

    return ManagedKafkaConsumer(topic, config, block)
}
