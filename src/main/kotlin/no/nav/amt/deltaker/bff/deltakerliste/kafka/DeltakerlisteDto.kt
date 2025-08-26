package no.nav.amt.deltaker.bff.deltakerliste.kafka

import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.deltaker.Arrangor
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDto(
    val id: UUID,
    val tiltakstype: TiltakstypeDto,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate? = null,
    val status: String,
    val virksomhetsnummer: String,
    val oppstart: Deltakerliste.Oppstartstype,
    val apentForPamelding: Boolean = false,
    val antallPlasser: Int,
) {
    data class TiltakstypeDto(
        val navn: String,
        val arenaKode: String,
    ) {
        fun erStottet() = this.arenaKode in setOf(
            "INDOPPFAG",
            "ARBFORB",
            "AVKLARAG",
            "VASV",
            "ARBRRHDAG",
            "DIGIOPPARB",
            "JOBBK",
            "GRUPPEAMO",
            "GRUFAGYRKE",
        )
    }

    fun toModel(arrangor: Arrangor, tiltakstype: Tiltakstype) = Deltakerliste(
        id = this.id,
        tiltak = tiltakstype,
        navn = this.navn,
        status = Deltakerliste.Status.fromString(this.status),
        startDato = this.startDato,
        sluttDato = this.sluttDato,
        oppstart = this.oppstart,
        arrangor = Deltakerliste.Arrangor(
            arrangor = arrangor,
            overordnetArrangorNavn = null,
        ),
        apentForPamelding = this.apentForPamelding,
        antallPlasser = this.antallPlasser,
    )
}
