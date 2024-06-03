package no.nav.amt.deltaker.bff.deltaker.navbruker

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

class NavBrukerConsumer(
    private val repository: NavBrukerRepository,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl("earliest"),
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_NAV_BRUKER_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        if (value == null) {
            log.warn("Mottok tombstone for nav-bruker: $key, skal ikke skje.")
            return
        }
        repository.upsert(objectMapper.readValue(value))
    }

    override fun run() = consumer.run()
}
