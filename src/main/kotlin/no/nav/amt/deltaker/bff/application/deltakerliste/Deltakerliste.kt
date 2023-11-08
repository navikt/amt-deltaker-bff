package no.nav.amt.deltaker.bff.application.deltakerliste

import java.time.LocalDate
import java.util.UUID

data class Deltakerliste(
    val id: UUID,
    val arrangorId: UUID,
    val tiltak: Tiltak,
    val navn: String,
    val status: Status,
    val startDato: LocalDate,
    val sluttDato: LocalDate? = null,
    val oppstart: Oppstartstype?,
) {
    enum class Oppstartstype {
        LOPENDE,
        FELLES,
    }

    enum class Status {
        GJENNOMFORES,
        AVBRUTT,
        AVLYST,
        AVSLUTTET,
        PLANLAGT,
        ;

        companion object {
            fun fromString(status: String) = when (status) {
                "GJENNOMFORES" -> Deltakerliste.Status.GJENNOMFORES
                "AVBRUTT" -> Deltakerliste.Status.AVBRUTT
                "AVLYST" -> Deltakerliste.Status.AVLYST
                "AVSLUTTET" -> Deltakerliste.Status.AVSLUTTET
                "PLANLAGT", "APENT_FOR_INNSOK" -> Deltakerliste.Status.PLANLAGT
                else -> error("Ukjent deltakerlistestatus: $status")
            }
        }
    }

    fun erKurs(): Boolean {
        if (oppstart != null) {
            return oppstart == Oppstartstype.FELLES
        } else {
            return kursTiltak.contains(tiltak.type)
        }
    }

    private val kursTiltak = setOf(
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
    )
}
