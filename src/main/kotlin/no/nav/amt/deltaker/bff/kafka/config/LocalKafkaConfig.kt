package no.nav.amt.deltaker.bff.kafka.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer

class LocalKafkaConfig(
    val kafkaBrokers: String = System.getenv("KAFKA_BROKERS") ?: "http://localhost:9092",
    val kafkaAutoOffsetReset: String = "earliest",
) : KafkaConfig {
    override fun commonConfig() = mapOf(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
    )

    override fun <K, V> consumerConfig(
        keyDeserializer: Deserializer<K>,
        valueDeserializer: Deserializer<V>,
        groupId: String,
    ) = mapOf(
        ConsumerConfig.GROUP_ID_CONFIG to groupId,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaAutoOffsetReset,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializer::class.java,
    ) + commonConfig()
}
