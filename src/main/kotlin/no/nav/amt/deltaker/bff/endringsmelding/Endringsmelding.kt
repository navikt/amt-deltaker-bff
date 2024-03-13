package no.nav.amt.deltaker.bff.endringsmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Endringsmelding(
    val id: UUID,
    val deltakerId: UUID,
    val utfortAvNavAnsattId: UUID?,
    val utfortTidspunkt: LocalDateTime?,
    val opprettetAvArrangorAnsattId: UUID,
    val createdAt: LocalDateTime,
    val status: Status,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = Innhold.LeggTilOppstartsdatoInnhold::class, name = "LEGG_TIL_OPPSTARTSDATO"),
        JsonSubTypes.Type(value = Innhold.EndreOppstartsdatoInnhold::class, name = "ENDRE_OPPSTARTSDATO"),
        JsonSubTypes.Type(value = Innhold.ForlengDeltakelseInnhold::class, name = "FORLENG_DELTAKELSE"),
        JsonSubTypes.Type(value = Innhold.AvsluttDeltakelseInnhold::class, name = "AVSLUTT_DELTAKELSE"),
        JsonSubTypes.Type(value = Innhold.DeltakerIkkeAktuellInnhold::class, name = "DELTAKER_IKKE_AKTUELL"),
        JsonSubTypes.Type(value = Innhold.EndreDeltakelseProsentInnhold::class, name = "ENDRE_DELTAKELSE_PROSENT"),
        JsonSubTypes.Type(value = Innhold.EndreSluttdatoInnhold::class, name = "ENDRE_SLUTTDATO"),
        JsonSubTypes.Type(value = Innhold.EndreSluttaarsakInnhold::class, name = "ENDRE_SLUTTAARSAK"),
    )
    val innhold: Innhold,
    val type: Type,
) {
    enum class Status {
        AKTIV,
        TILBAKEKALT,
        UTDATERT,
        UTFORT,
    }

    enum class Type {
        LEGG_TIL_OPPSTARTSDATO,
        ENDRE_OPPSTARTSDATO,
        FORLENG_DELTAKELSE,
        AVSLUTT_DELTAKELSE,
        DELTAKER_IKKE_AKTUELL,
        ENDRE_DELTAKELSE_PROSENT,
        ENDRE_SLUTTDATO,
        ENDRE_SLUTTAARSAK,
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = Innhold.LeggTilOppstartsdatoInnhold::class, name = "LEGG_TIL_OPPSTARTSDATO"),
        JsonSubTypes.Type(value = Innhold.EndreOppstartsdatoInnhold::class, name = "ENDRE_OPPSTARTSDATO"),
        JsonSubTypes.Type(value = Innhold.ForlengDeltakelseInnhold::class, name = "FORLENG_DELTAKELSE"),
        JsonSubTypes.Type(value = Innhold.AvsluttDeltakelseInnhold::class, name = "AVSLUTT_DELTAKELSE"),
        JsonSubTypes.Type(value = Innhold.DeltakerIkkeAktuellInnhold::class, name = "DELTAKER_IKKE_AKTUELL"),
        JsonSubTypes.Type(value = Innhold.EndreDeltakelseProsentInnhold::class, name = "ENDRE_DELTAKELSE_PROSENT"),
        JsonSubTypes.Type(value = Innhold.EndreSluttdatoInnhold::class, name = "ENDRE_SLUTTDATO"),
        JsonSubTypes.Type(value = Innhold.EndreSluttaarsakInnhold::class, name = "ENDRE_SLUTTAARSAK"),
    )
    sealed class Innhold {
        data class LeggTilOppstartsdatoInnhold(
            val oppstartsdato: LocalDate,
        ) : Innhold()

        data class EndreOppstartsdatoInnhold(
            val oppstartsdato: LocalDate?,
        ) : Innhold()

        data class ForlengDeltakelseInnhold(
            val sluttdato: LocalDate,
        ) : Innhold()

        data class AvsluttDeltakelseInnhold(
            val sluttdato: LocalDate,
            val aarsak: EndringsmeldingStatusAarsak,
        ) : Innhold()

        data class DeltakerIkkeAktuellInnhold(
            val aarsak: EndringsmeldingStatusAarsak,
        ) : Innhold()

        data class EndreDeltakelseProsentInnhold(
            val deltakelseProsent: Int,
            val dagerPerUke: Int?,
            val gyldigFraDato: LocalDate?,
        ) : Innhold()

        data class EndreSluttdatoInnhold(
            val sluttdato: LocalDate,
        ) : Innhold()

        data class EndreSluttaarsakInnhold(
            val aarsak: EndringsmeldingStatusAarsak,
        ) : Innhold()
    }
}

data class EndringsmeldingStatusAarsak(
    val type: Type,
    val beskrivelse: String? = null,
) {
    init {
        if (beskrivelse != null && type != Type.ANNET) {
            error("Aarsak $type skal ikke ha beskrivelse")
        }
    }

    enum class Type {
        SYK,
        FATT_JOBB,
        TRENGER_ANNEN_STOTTE,
        UTDANNING,
        IKKE_MOTT,
        ANNET,
    }
}
