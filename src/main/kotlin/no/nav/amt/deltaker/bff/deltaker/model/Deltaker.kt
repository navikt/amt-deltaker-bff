package no.nav.amt.deltaker.bff.deltaker.model

import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import java.time.LocalDate
import java.util.UUID

data class Deltaker(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerliste: Deltakerliste,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val status: DeltakerStatus,
    val historikk: List<DeltakerHistorikk>,
) {
    val fattetVedtak = historikk.firstOrNull {
        it is DeltakerHistorikk.Vedtak && it.vedtak.gyldigTil == null && it.vedtak.fattet != null
    }?.let { (it as DeltakerHistorikk.Vedtak).vedtak }

    val ikkeFattetVedtak = historikk.firstOrNull {
        it is DeltakerHistorikk.Vedtak && it.vedtak.fattet == null
    }?.let { (it as DeltakerHistorikk.Vedtak).vedtak }

    val vedtaksinformasjon = if (this.fattetVedtak != null) {
        fattetVedtak
    } else {
        ikkeFattetVedtak
    }

    fun getDeltakerHistorikSortert() = historikk.sortedByDescending { it.sistEndret }

    fun harSluttet(): Boolean {
        return status.type in AVSLUTTENDE_STATUSER
    }

    fun deltarPaKurs(): Boolean {
        return deltakerliste.erKurs()
    }
}

data class Innhold(
    val tekst: String,
    val innholdskode: String,
    val valgt: Boolean,
    val beskrivelse: String?,
)
