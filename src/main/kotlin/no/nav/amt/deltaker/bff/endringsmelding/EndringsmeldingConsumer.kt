package no.nav.amt.deltaker.bff.endringsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class EndringsmeldingConsumer(
    private val endringsmeldingService: EndringsmeldingService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_ENDRINGSMELDING_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consumeEndringsmelding,
    )

    suspend fun consumeEndringsmelding(id: UUID, melding: String?) {
        if (melding == null) {
            endringsmeldingService.delete(id)
        } else {
            endringsmeldingService.upsert(objectMapper.readValue(melding))
        }
    }

    fun run() {
        log.info("Initialiserer endringsmelding-consumer")
        consumer.run()
    }
}
