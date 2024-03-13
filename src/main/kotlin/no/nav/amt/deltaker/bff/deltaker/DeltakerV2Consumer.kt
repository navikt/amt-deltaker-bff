package no.nav.amt.deltaker.bff.deltaker

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.model.Dataelement
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.kafka.Consumer
import no.nav.amt.deltaker.bff.kafka.ManagedKafkaConsumer
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfig
import no.nav.amt.deltaker.bff.kafka.config.KafkaConfigImpl
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DeltakerV2Consumer(
    private val deltakerService: DeltakerService,
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

        if (deltakerV2.kilde != DeltakerV2Dto.Kilde.KOMET) return

        deltakerService.oppdaterDeltaker(deltakerV2.toDeltakerOppdatering())
        deltakerService.oppdaterTilgjengeligeData(deltakerV2.id, deltakerV2.dataelementer)
    }

    override fun run() = consumer.run()
}

data class DeltakerV2Dto(
    val id: UUID,
    val status: DeltakerStatusDto,
    val dagerPerUke: Float?,
    val prosentStilling: Double?,
    val oppstartsdato: LocalDate?,
    val sluttdato: LocalDate?,
    val bestillingTekst: String?,
    val kilde: Kilde?,
    val innhold: Deltakelsesinnhold?,
    val historikk: List<DeltakerHistorikk>?,
    val personalia: PersonaliaDto,
    val navVeileder: Any?,
    val navKontor: String?,
) {
    val dataelementer: List<Dataelement>
        get() {
            fun elementOrNull(it: Any?, element: Dataelement) = if (it == null) null else element
            val innholdselement = if (innhold?.innhold.isNullOrEmpty()) null else Dataelement.INNHOLD

            return listOfNotNull(
                Dataelement.NAVN,
                Dataelement.PERSONIDENT,
                elementOrNull(navVeileder, Dataelement.NAV_VEILEDER),
                elementOrNull(navKontor, Dataelement.NAV_KONTOR),
                elementOrNull(personalia.adresse, Dataelement.ADRESSE),
                elementOrNull(personalia.kontaktinformasjon.telefonnummer, Dataelement.TELEFON),
                elementOrNull(personalia.kontaktinformasjon.epost, Dataelement.EPOST),
                elementOrNull(bestillingTekst, Dataelement.BAKGRUNNSINFO),
                innholdselement,
            )
        }

    fun toDeltakerOppdatering(): Deltakeroppdatering {
        require(status.id != null) { "Kan ikke håndtere deltakerstatus uten id for deltaker $id" }
        require(historikk != null) { "Kan ikke håndtere deltaker $id uten historikk" }

        return Deltakeroppdatering(
            id = id,
            startdato = oppstartsdato,
            sluttdato = sluttdato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = prosentStilling?.toFloat(),
            bakgrunnsinformasjon = bestillingTekst,
            innhold = innhold?.innhold ?: emptyList(),
            status = DeltakerStatus(
                id = status.id,
                type = status.type,
                aarsak = status.aarsak?.let { DeltakerStatus.Aarsak(it, status.aarsaksbeskrivelse) },
                gyldigFra = status.gyldigFra,
                gyldigTil = null,
                opprettet = status.opprettetDato,
            ),
            historikk = historikk,
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

    data class Deltakelsesinnhold(
        val ledetekst: String,
        val innhold: List<Innhold>,
    )

    data class PersonaliaDto(
        val kontaktinformasjon: DeltakerKontaktinformasjonDto,
        val adresse: Any?,
    )

    data class DeltakerKontaktinformasjonDto(
        val telefonnummer: String?,
        val epost: String?,
    )
}
