package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.getInnholdselementerMedAnnet
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.toInnhold

data class InnholdDto(
    val innholdskode: String,
    val beskrivelse: String?,
)

fun finnValgtInnhold(innhold: List<InnholdDto>, deltaker: Deltaker) = innhold.mapNotNull { innholdDto ->
    val tiltaksinnhold = deltaker.deltakerliste.tiltak.innhold
        ?.getInnholdselementerMedAnnet(deltaker.deltakerliste.tiltak.tiltakskode)
        ?.find { it.innholdskode == innholdDto.innholdskode }
    if (innholdDto.innholdskode == annetInnholdselement.innholdskode) {
        tiltaksinnhold?.toInnhold(true, innholdDto.beskrivelse)
    } else {
        tiltaksinnhold?.toInnhold(valgt = true)
    }
}
