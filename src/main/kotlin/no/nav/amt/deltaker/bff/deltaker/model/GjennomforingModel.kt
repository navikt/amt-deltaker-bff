package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.apiclients.deltaker.GjennomforingResponse
import no.nav.amt.lib.models.deltakerliste.GjennomforingPameldingType
import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.util.UUID

data class GjennomforingModel(
    val id: UUID,
    val tiltak: Tiltakstype,
    val navn: String,
    val status: GjennomforingStatusType?,
    val startDato: LocalDate?,
    val sluttDato: LocalDate? = null,
    val oppstart: Oppstartstype?,
    val arrangor: ArrangorModel,
    val apentForPamelding: Boolean,
    val antallPlasser: Int?,
    val oppmoteSted: String?,
    val pameldingstype: GjennomforingPameldingType?,
) {
    companion object {
        fun fromGjennomforingResponse(gjennomforing: GjennomforingResponse): GjennomforingModel = GjennomforingModel(
            id = gjennomforing.id,
            tiltak = gjennomforing.tiltakstype,
            navn = gjennomforing.navn,
            status = gjennomforing.status,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            oppstart = gjennomforing.oppstart,
            arrangor = ArrangorModel(
                navn = gjennomforing.arrangor.navn,
            ),
            apentForPamelding = gjennomforing.apentForPamelding,
            antallPlasser = gjennomforing.antallPlasser,
            oppmoteSted = gjennomforing.oppmoteSted,
            pameldingstype = gjennomforing.pameldingstype,
        )
    }
}
