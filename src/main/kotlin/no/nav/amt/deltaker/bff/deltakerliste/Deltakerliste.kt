package no.nav.amt.deltaker.bff.deltakerliste

import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.util.UUID

data class Deltakerliste(
    val id: UUID,
    val tiltak: Tiltakstype,
    val navn: String,
    val status: Status,
    val startDato: LocalDate,
    val sluttDato: LocalDate? = null,
    val oppstart: Oppstartstype?,
    val arrangor: Arrangor,
) {
    data class Arrangor(
        val arrangor: no.nav.amt.deltaker.bff.arrangor.Arrangor,
        val overordnetArrangorNavn: String?,
    )

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
                "GJENNOMFORES" -> GJENNOMFORES
                "AVBRUTT" -> AVBRUTT
                "AVLYST" -> AVLYST
                "AVSLUTTET" -> AVSLUTTET
                "PLANLAGT", "APENT_FOR_INNSOK" -> PLANLAGT
                else -> error("Ukjent deltakerlistestatus: $status")
            }
        }
    }

    fun getOppstartstype(): Oppstartstype {
        return oppstart
            ?: if (erKurs()) {
                Oppstartstype.FELLES
            } else {
                Oppstartstype.LOPENDE
            }
    }

    fun erKurs(): Boolean {
        if (oppstart != null) {
            return oppstart == Oppstartstype.FELLES
        } else {
            return kursTiltak.contains(tiltak.type)
        }
    }

    fun deltakerAdresseDeles() = tiltakUtenDeltakerAdresset.contains(this.tiltak.type)
}

private val kursTiltak = setOf(
    Tiltak.Type.JOBBK,
    Tiltak.Type.GRUPPEAMO,
    Tiltak.Type.GRUFAGYRKE,
)

private val tiltakUtenDeltakerAdresset = setOf(
    Tiltak.Type.DIGIOPPARB,
    Tiltak.Type.JOBBK,
    Tiltak.Type.GRUPPEAMO,
    Tiltak.Type.GRUFAGYRKE,
)
