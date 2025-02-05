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
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
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
        ;

        companion object {
            fun fromString(status: String) = when (status) {
                "GJENNOMFORES" -> GJENNOMFORES
                "AVBRUTT" -> AVBRUTT
                "AVLYST" -> AVLYST
                "AVSLUTTET" -> AVSLUTTET
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

    private fun erKurs(): Boolean {
        return if (oppstart != null) {
            oppstart == Oppstartstype.FELLES
        } else {
            tiltak.erKurs()
        }
    }

    fun deltakerAdresseDeles() = !tiltakUtenDeltakerAdresset.contains(this.tiltak.arenaKode)
}

private val tiltakUtenDeltakerAdresset = setOf(
    Tiltakstype.ArenaKode.DIGIOPPARB,
    Tiltakstype.ArenaKode.JOBBK,
    Tiltakstype.ArenaKode.GRUPPEAMO,
    Tiltakstype.ArenaKode.GRUFAGYRKE,
)
