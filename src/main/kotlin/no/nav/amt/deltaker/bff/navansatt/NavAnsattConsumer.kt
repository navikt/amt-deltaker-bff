package no.nav.amt.deltaker.bff.navansatt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class NavAnsattConsumer(
    private val navAnsattService: NavAnsattService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_NAV_ANSATT_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        if (value == null) {
            navAnsattService.slettNavAnsatt(key)
            log.info("Slettet navansatt med id $key")
        } else {
            val dto = objectMapper.readValue<NavAnsattDto>(value)
            navAnsattService.oppdaterNavAnsatt(dto.toModel())
            log.info("Lagret navansatt med id $key")
        }
    }

    override fun start() = consumer.start()

    override suspend fun close() = consumer.close()
}

data class NavAnsattDto(
    val id: UUID,
    val navident: String,
    val navn: String,
    val epost: String?,
    val telefon: String?,
) {
    fun toModel() = NavAnsatt(id, navident, navn, epost, telefon)
}
