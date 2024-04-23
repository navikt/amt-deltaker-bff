package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.kafka.Consumer
import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class TiltakstypeConsumer(
    private val repository: TiltakstypeRepository,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String?> {
    private val consumer = ManagedKafkaConsumer(
        topic = Environment.TILTAKSTYPE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override fun run() = consumer.run()

    override suspend fun consume(key: UUID, value: String?) {
        value?.let { handterTiltakstype(objectMapper.readValue(it)) }
    }

    private fun handterTiltakstype(tiltakstype: TiltakstypeDto) {
        val stottedeTiltak = Tiltakstype.ArenaKode.entries.map { it.name }
        val arenaKode = tiltakstype.arenaKode
        if (arenaKode !in stottedeTiltak || tiltakstype.status != Tiltakstypestatus.Aktiv) return

        repository.upsert(tiltakstype.toModel(arenaKode!!))
    }
}
