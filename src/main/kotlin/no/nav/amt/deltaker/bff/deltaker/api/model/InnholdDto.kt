package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.getInnholdselementer
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.toInnhold

data class InnholdDto(
    val innholdskode: String,
    val beskrivelse: String?,
)

fun List<InnholdDto>.toInnholdModel(deltaker: Deltaker) = this.mapNotNull { valgtInnholdElement ->
    val tiltaksinnhold = getInnholdselementer(
        deltaker.deltakerliste.tiltak.innhold
            ?.innholdselementer,
        deltaker.deltakerliste.tiltak.tiltakskode,
    ).find { it.innholdskode == valgtInnholdElement.innholdskode }
    if (valgtInnholdElement.innholdskode == annetInnholdselement.innholdskode) {
        tiltaksinnhold?.toInnhold(true, valgtInnholdElement.beskrivelse)
    } else {
        tiltaksinnhold?.toInnhold(valgt = true)
    }
}
