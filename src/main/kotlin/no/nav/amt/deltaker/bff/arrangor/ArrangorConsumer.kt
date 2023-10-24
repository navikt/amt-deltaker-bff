package no.nav.amt.deltaker.bff.arrangor

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

class ArrangorConsumer(
    private val repository: ArrangorRepository,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.ARRANGOR_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consumeArrangor,
    )

    fun consumeArrangor(id: UUID, arrangor: String?) {
        if (arrangor == null) {
            repository.delete(id)
            log.info("Slettet arrangør med id $id")
        } else {
            repository.upsert(objectMapper.readValue(arrangor))
            log.info("Lagret arrangør med id $id")
        }
    }

    fun run() {
        consumer.run()
    }
}
