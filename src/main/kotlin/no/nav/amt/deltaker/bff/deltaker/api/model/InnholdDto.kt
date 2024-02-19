package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class InnholdDto(
    val innholdskode: String,
    val beskrivelse: String?,
)

fun finnValgtInnhold(
    innhold: List<InnholdDto>,
    deltaker: Deltaker,
) = innhold.mapNotNull { i ->
    val tiltaksinnhold = deltaker.deltakerliste.tiltak.innhold?.innholdselementer
        ?.find { it.innholdskode == i.innholdskode }
    tiltaksinnhold?.toInnhold(valgt = true)
}
