package no.nav.amt.deltaker.bff.utils

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

/**
 * Oppretter instans av [ManagedKafkaConsumer] med [UUID] som nøkkel og
 * nullable [String] som verdi.
 *
 * @param topic
 * @param consumerGroupId
 */
fun buildManagedKafkaConsumer(
    topic: String,
    consumerGroupId: String = Environment.KAFKA_CONSUMER_GROUP_ID,
    consumeFunc: suspend (key: UUID, value: String?) -> Unit,
): ManagedKafkaConsumer<UUID, String?> {
    val kafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl()

    return ManagedKafkaConsumer(
        topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = consumerGroupId,
        ),
        consume = consumeFunc,
    )
}
