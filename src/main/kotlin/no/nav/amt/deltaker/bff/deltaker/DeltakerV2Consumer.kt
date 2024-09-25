package no.nav.amt.deltaker.bff.deltaker

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DeltakerV2Consumer(
    private val deltakerService: DeltakerService,
    private val deltakerlisteRepository: DeltakerlisteRepository,
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

        val tiltakstype = deltakerlisteRepository.get(deltakerV2.deltakerlisteId).getOrThrow().tiltak.arenaKode

        if (tiltakstype != Tiltakstype.ArenaKode.ARBFORB) {
            log.info("Ignorerer deltaker $key som ikke har tiltakstype ARBFORB")
            return
        }

        // egen håndtering for deltaker med kilde arena som ikke finnes fra før?
        // husk:
        // - Hvis det finnes tidligere deltakelser på samme tiltak må disse settes til at ikke kan endres
        // - Hvis det finnes nyere deltakelser på samme tiltak og mottatt deltakelse har avsluttende status må mottatt deltakelse settes til at ikke kan endres
        // vi må nok inserte navbruker hvis den ikke finnes fra før
        deltakerService.oppdaterDeltaker(
            deltakeroppdatering = deltakerV2.toDeltakerOppdatering(),
        )
    }

    override fun run() = consumer.run()
}

data class DeltakerV2Dto(
    val id: UUID,
    val deltakerlisteId: UUID,
    val status: DeltakerStatusDto,
    val dagerPerUke: Float?,
    val prosentStilling: Double?,
    val oppstartsdato: LocalDate?,
    val sluttdato: LocalDate?,
    val bestillingTekst: String?,
    val kilde: Kilde?,
    val innhold: Deltakelsesinnhold?,
    val historikk: List<DeltakerHistorikk>?,
    val forcedUpdate: Boolean? = false,
) {
    fun toDeltakerOppdatering(): Deltakeroppdatering {
        require(status.id != null) { "Kan ikke håndtere deltakerstatus uten id for deltaker $id" }

        return Deltakeroppdatering(
            id = id,
            startdato = oppstartsdato,
            sluttdato = sluttdato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = prosentStilling?.toFloat(),
            bakgrunnsinformasjon = bestillingTekst,
            deltakelsesinnhold = innhold,
            status = DeltakerStatus(
                id = status.id,
                type = status.type,
                aarsak = status.aarsak?.let { DeltakerStatus.Aarsak(it, status.aarsaksbeskrivelse) },
                gyldigFra = status.gyldigFra,
                gyldigTil = null,
                opprettet = status.opprettetDato,
            ),
            historikk = historikk.orEmpty(),
            forcedUpdate = forcedUpdate,
        )
    }

    enum class Kilde {
        KOMET,
        ARENA,
    }

    data class DeltakerStatusDto(
        val id: UUID?,
        val type: DeltakerStatus.Type,
        val aarsak: DeltakerStatus.Aarsak.Type?,
        val aarsaksbeskrivelse: String?,
        val gyldigFra: LocalDateTime,
        val opprettetDato: LocalDateTime,
    )
}
