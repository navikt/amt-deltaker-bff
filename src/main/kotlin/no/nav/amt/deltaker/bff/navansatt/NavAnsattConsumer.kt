package no.nav.amt.deltaker.bff.navansatt

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

class NavAnsattConsumer(
    private val navAnsattService: NavAnsattService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_NAV_ANSATT_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consumeNavAnsatt,
    )

    fun consumeNavAnsatt(id: UUID, navAnsatt: String?) {
        if (navAnsatt == null) {
            navAnsattService.slettNavAnsatt(id)
            log.info("Slettet navansatt med id $id")
        } else {
            val dto = objectMapper.readValue<NavAnsattDto>(navAnsatt)
            navAnsattService.oppdaterNavAnsatt(dto.toModel())
            log.info("Lagret navansatt med id $id")
        }
    }

    fun run() = consumer.run()
}

data class NavAnsattDto(
    val id: UUID,
    val navident: String,
    val navn: String,
) {
    fun toModel() = NavAnsatt(id, navident, navn)
}
