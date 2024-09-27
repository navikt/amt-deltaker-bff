package no.nav.amt.deltaker.bff.deltaker.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerV2Consumer(
    private val deltakerService: DeltakerService,
    private val deltakerlisteRepository: DeltakerlisteRepository,
    private val navBrukerService: NavBrukerService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.AMT_DELTAKERV2_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        if (value == null) {
            log.info("Mottok tombstone for deltaker $key - håndterer ikke meldingen")
            return
        }

        val deltakerV2 = objectMapper.readValue<DeltakerV2Dto>(value)
        val deltakerliste = deltakerlisteRepository.get(deltakerV2.deltakerlisteId).getOrThrow()
        val tiltakstype = deltakerliste.tiltak.arenaKode

        if (tiltakstype != Tiltakstype.ArenaKode.ARBFORB) {
            log.info("Ignorerer deltaker $key som ikke har tiltakstype ARBFORB")
            return
        }

        val deltakerFinnes = deltakerService.deltakerFinnes(deltakerV2.id)
        if (deltakerFinnes || deltakerV2.kilde == DeltakerV2Dto.Kilde.KOMET) {
            log.info("Oppdaterer deltaker med id ${deltakerV2.id}")
            deltakerService.oppdaterDeltaker(
                deltakeroppdatering = deltakerV2.toDeltakerOppdatering(),
            )
        } else {
            log.info("Inserter ny deltaker med id ${deltakerV2.id}")
            val navBruker = navBrukerService.getOrCreate(deltakerV2.personalia.personident).getOrThrow()
            val deltaker = deltakerV2.toDeltaker(navBruker, deltakerliste)
            deltakerService.opprettArenaDeltaker(deltaker)
        }
    }

    override fun run() = consumer.run()
}