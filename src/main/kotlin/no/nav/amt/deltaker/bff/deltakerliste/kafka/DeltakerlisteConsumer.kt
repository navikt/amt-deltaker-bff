package no.nav.amt.deltaker.bff.deltakerliste.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.utils.KafkaConsumerFactory.buildManagedKafkaConsumer
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerlisteConsumer(
    private val deltakerlisteRepository: DeltakerlisteRepository,
    private val arrangorService: ArrangorService,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val pameldingService: PameldingService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val unleashToggle: UnleashToggle,
    private val topic: String,
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = buildManagedKafkaConsumer(
        topic = topic,
        consumeFunc = ::consume,
    )

    override fun start() = consumer.start()

    override suspend fun close() = consumer.close()

    override suspend fun consume(key: UUID, value: String?) {
        if (topic == Environment.DELTAKERLISTE_V2_TOPIC && !unleashToggle.skalLeseGjennomforingerV2()) {
            return
        }

        if (value == null) {
            deltakerlisteRepository.delete(key)
        } else {
            handterDeltakerliste(objectMapper.readValue(value))
        }
    }

    private suspend fun handterDeltakerliste(deltakerlistePayload: DeltakerlistePayload) {
        if (unleashToggle.skipProsesseringAvGjennomforing(deltakerlistePayload.tiltakstype.tiltakskode)) {
            return
        }

        val tiltakskode = Tiltakskode.valueOf(deltakerlistePayload.tiltakstype.tiltakskode)

        val deltakerliste = deltakerlistePayload.toModel(
            arrangor = arrangorService.hentArrangor(deltakerlistePayload.organisasjonsnummer),
            tiltakstype = tiltakstypeRepository.get(tiltakskode).getOrThrow(),
        )

        deltakerlisteRepository.upsert(deltakerliste)

        if (deltakerliste.status == Deltakerliste.Status.AVLYST || deltakerliste.status == Deltakerliste.Status.AVBRUTT) {
            val kladderSomSkalSlettes = pameldingService.getKladderForDeltakerliste(deltakerliste.id)
            kladderSomSkalSlettes.forEach {
                pameldingService.slettKladd(it)
            }
            log.info("Slettet ${kladderSomSkalSlettes.size} for deltakerliste ${deltakerliste.id} med status ${deltakerliste.status.name}")

            tilgangskontrollService.stengTilgangerTilDeltakerliste(deltakerliste.id)
        }
    }
}
