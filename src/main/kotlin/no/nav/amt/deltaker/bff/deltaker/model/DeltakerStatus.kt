package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDateTime
import java.util.UUID

data class DeltakerStatus(
    val id: UUID,
    val type: Type,
    val aarsak: Aarsak?,
    val gyldigFra: LocalDateTime,
    val gyldigTil: LocalDateTime?,
    val opprettet: LocalDateTime,
) {
    data class Aarsak(
        val type: Type,
        val beskrivelse: String?,
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
            FIKK_IKKE_PLASS,
            IKKE_MOTT,
            ANNET,
            AVLYST_KONTRAKT,
        }
    }

    enum class Type {
        KLADD,
        UTKAST_TIL_PAMELDING,
        AVBRUTT_UTKAST,
        VENTER_PA_OPPSTART,
        DELTAR,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        FEILREGISTRERT,
        SOKT_INN,
        VURDERES,
        VENTELISTE,
        AVBRUTT,
        FULLFORT,
        PABEGYNT_REGISTRERING,
    }
}

val AVSLUTTENDE_STATUSER = listOf(
    DeltakerStatus.Type.HAR_SLUTTET,
    DeltakerStatus.Type.IKKE_AKTUELL,
    DeltakerStatus.Type.FEILREGISTRERT,
    DeltakerStatus.Type.AVBRUTT,
    DeltakerStatus.Type.FULLFORT,
    DeltakerStatus.Type.AVBRUTT_UTKAST,
)

val AKTIVE_STATUSER = listOf(
    DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
    DeltakerStatus.Type.VENTER_PA_OPPSTART,
    DeltakerStatus.Type.DELTAR,
)
