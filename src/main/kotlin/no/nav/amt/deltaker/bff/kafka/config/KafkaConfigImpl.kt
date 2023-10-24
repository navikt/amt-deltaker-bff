package no.nav.amt.deltaker.bff.kafka.config

import no.nav.amt.deltaker.bff.getEnvVar
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringSerializer

class KafkaConfigImpl : KafkaConfig {
    private val javaKey = "JKS"
    private val pkcs12 = "PKCS12"
    private val autoOffsetReset: String = "latest"

    private val kafkaBrokers = getEnvVar("KAFKA_BROKERS")
    private val kafkaTruststorePath = getEnvVar("KAFKA_TRUSTSTORE_PATH")
    private val kafkaCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD")
    private val kafkaKeystorePath = getEnvVar("KAFKA_KEYSTORE_PATH")

    override fun commonConfig() = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
    ) + securityConfig()

    private fun securityConfig() = mapOf(
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to javaKey,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to pkcs12,
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
    )

    override fun <K, V> consumerConfig(
        keyDeserializer: Deserializer<K>,
        valueDeserializer: Deserializer<V>,
        groupId: String,
    ) = mapOf(
        ConsumerConfig.GROUP_ID_CONFIG to groupId,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializer::class.java,
    ) + commonConfig()
}
