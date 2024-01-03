package no.nav.amt.deltaker.bff.deltaker.kafka

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class DeltakerProducer(
    private val kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun produce(deltaker: Deltaker) {
        if (deltaker.status.type == DeltakerStatus.Type.KLADD) return

        val key = deltaker.id.toString()
        val value = objectMapper.writeValueAsString(deltaker.toDto())
        val record = ProducerRecord(Environment.DELTAKER_ENDRING_TOPIC, key, value)

        KafkaProducer<String, String>(kafkaConfig.producerConfig()).use {
            val metadata = it.send(record).get()
            log.info(
                "Produserte melding til topic ${metadata.topic()}, " +
                    "key=$key, " +
                    "offset=${metadata.offset()}, " +
                    "partition=${metadata.partition()}",
            )
        }
    }
}
