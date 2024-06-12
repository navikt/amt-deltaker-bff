package no.nav.amt.deltaker.bff.deltakerliste.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerlisteConsumer(
    private val repository: DeltakerlisteRepository,
    private val arrangorService: ArrangorService,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val pameldingService: PameldingService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.DELTAKERLISTE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override fun run() = consumer.run()

    override suspend fun consume(key: UUID, value: String?) {
        if (value == null) {
            repository.delete(key)
        } else {
            handterDeltakerliste(objectMapper.readValue(value))
        }
    }

    private suspend fun handterDeltakerliste(deltakerlisteDto: DeltakerlisteDto) {
        if (!deltakerlisteDto.tiltakstype.erStottet()) return

        val arrangor = arrangorService.hentArrangor(deltakerlisteDto.virksomhetsnummer)
        val tiltakstype = tiltakstypeRepository.get(Tiltakstype.ArenaKode.valueOf(deltakerlisteDto.tiltakstype.arenaKode)).getOrThrow()
        val deltakerliste = deltakerlisteDto.toModel(arrangor, tiltakstype)
        repository.upsert(deltakerliste)

        if (deltakerliste.status == Deltakerliste.Status.AVLYST || deltakerliste.status == Deltakerliste.Status.AVBRUTT) {
            val kladderSomSkalSlettes = pameldingService.getKladderForDeltakerliste(deltakerliste.id)
            kladderSomSkalSlettes.forEach {
                pameldingService.slettKladd(it)
            }
            log.info("Slettet ${kladderSomSkalSlettes.size} for deltakerliste ${deltakerliste.id} med status ${deltakerliste.status.name}")
        }
    }
}
