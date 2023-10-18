package no.nav.amt.deltaker.bff.kafka

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import no.nav.amt.deltaker.bff.utils.AsyncUtils
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.apache.kafka.common.serialization.UUIDSerializer
import org.junit.Test
import java.util.Properties
import java.util.UUID

class ManagedKafkaConsumerTest {
    val topic = "test.topic"

    val stringConsumerConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost()).consumerConfig(
        keyDeserializer = StringDeserializer(),
        valueDeserializer = StringDeserializer(),
        groupId = "test-consumer",
    )

    @Test
    fun `ManagedKafkaConsumer - konsumerer record med String, String`() {
        val key = "key"
        val value = "value"
        val cache = mutableMapOf<String, String>()

        produce(ProducerRecord(topic, key, value))

        val consumer = ManagedKafkaConsumer(topic, stringConsumerConfig) { k: String, v: String ->
            cache[k] = v
        }
        consumer.run()

        AsyncUtils.eventually {
            cache[key] shouldBe value
            consumer.stop()
        }
    }

    @Test
    fun `ManagedKafkaConsumer - konsumerer record med UUID, ByteArray`() {
        val key = UUID.randomUUID()
        val value = "value".toByteArray()
        val cache = mutableMapOf<UUID, ByteArray>()
        val uuidTopic = "uuid.topic"

        produceUUIDByteArray(ProducerRecord(uuidTopic, key, value))

        val config = LocalKafkaConfig(SingletonKafkaProvider.getHost())
            .consumerConfig(
                keyDeserializer = UUIDDeserializer(),
                valueDeserializer = ByteArrayDeserializer(),
                groupId = "test-consumer",
            )

        val consumer = ManagedKafkaConsumer(uuidTopic, config) { k: UUID, v: ByteArray ->
            cache[k] = v
        }
        consumer.run()

        AsyncUtils.eventually {
            cache[key] shouldBe value
            consumer.stop()
        }
    }

    @Test
    fun `ManagedKafkaConsumer - prøver å konsumere melding på nytt hvis noe feiler`() {
        val key = "key"
        val value = "value"

        var antallGangerKallt = 0

        produce(ProducerRecord(topic, key, value))

        val consumer = ManagedKafkaConsumer<String, String>(topic, stringConsumerConfig) { _, _ ->
            antallGangerKallt++
            throw IllegalStateException("skal feile noen ganger")
        }
        consumer.run()

        AsyncUtils.eventually {
            antallGangerKallt shouldBe 2
            consumer.stop()
        }
    }

    private fun produce(record: ProducerRecord<String, String>): RecordMetadata {
        KafkaProducer<String, String>(
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SingletonKafkaProvider.getHost())
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            },
        ).use { producer ->
            return producer.send(record).get()
        }
    }

    private fun produceUUIDByteArray(record: ProducerRecord<UUID, ByteArray>): RecordMetadata {
        KafkaProducer<UUID, ByteArray>(
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SingletonKafkaProvider.getHost())
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
            },
        ).use { producer ->
            return producer.send(record).get()
        }
    }
}
