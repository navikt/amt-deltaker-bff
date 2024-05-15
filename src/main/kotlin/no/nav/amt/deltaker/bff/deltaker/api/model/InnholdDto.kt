package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement

data class InnholdDto(
    val innholdskode: String,
    val beskrivelse: String?,
)

fun finnValgtInnhold(innhold: List<InnholdDto>, deltaker: Deltaker) = innhold.mapNotNull { innholdDto ->
    val tiltaksinnhold = deltaker.deltakerliste.tiltak.innhold?.innholdselementerMedAnnet
        ?.find { it.innholdskode == innholdDto.innholdskode }
    if (innholdDto.innholdskode == annetInnholdselement.innholdskode) {
        tiltaksinnhold?.toInnhold(true, innholdDto.beskrivelse)
    } else {
        tiltaksinnhold?.toInnhold(valgt = true)
    }
}
