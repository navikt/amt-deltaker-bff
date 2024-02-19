package no.nav.amt.deltaker.bff.deltaker.api.utils

import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement

const val MAX_BAKGRUNNSINFORMASJON_LENGDE = 1000
const val MAX_ANNET_INNHOLD_LENGDE = 250
const val MAX_AARSAK_BESKRIVELSE_LENGDE = 40
const val MIN_DAGER_PER_UKE = 1
const val MAX_DAGER_PER_UKE = 5
const val MIN_DELTAKELSESPROSENT = 1
const val MAX_DELTAKELSESPROSENT = 100

fun validerBakgrunnsinformasjon(tekst: String?) = tekst?.let {
    require(it.length <= MAX_BAKGRUNNSINFORMASJON_LENGDE) {
        "Bakgrunnsinformasjon kan ikke være lengre enn $MAX_BAKGRUNNSINFORMASJON_LENGDE"
    }
}

fun validerAnnetInnhold(tekst: String?) = tekst?.let {
    require(it.length <= MAX_ANNET_INNHOLD_LENGDE) {
        "Begrunnelse kan ikke være lengre enn $MAX_ANNET_INNHOLD_LENGDE"
    }
}

fun validerAarsaksBeskrivelse(tekst: String?) = tekst?.let {
    require(tekst.length <= MAX_AARSAK_BESKRIVELSE_LENGDE) {
        "Beskrivelse kan ikke være lengre enn $MAX_AARSAK_BESKRIVELSE_LENGDE"
    }
}

fun validerDagerPerUke(n: Int?) = n?.let {
    require(n in MIN_DAGER_PER_UKE..MAX_DAGER_PER_UKE) {
        "Dager per uke kan ikke være mindre enn $MIN_DAGER_PER_UKE eller større enn $MAX_DAGER_PER_UKE"
    }
}

fun validerDeltakelsesProsent(n: Int?) = n?.let {
    require(n in MIN_DELTAKELSESPROSENT..MAX_DELTAKELSESPROSENT) {
        "Deltakelsesprosent kan ikke være mindre enn $MIN_DELTAKELSESPROSENT eller større enn $MAX_DELTAKELSESPROSENT"
    }
}

fun validerInnhold(innhold: List<InnholdDto>, tiltaksinnhold: DeltakerRegistreringInnhold?) {
    require(tiltaksinnhold != null) {
        "Kan ikke validere innhold for tiltakstype uten innhold"
    }

    val innholdskoder = tiltaksinnhold
        .innholdselementer
        .map { it.innholdskode }
        .plus(annetInnholdselement.innholdskode)

    innhold.forEach {
        require(it.innholdskode in innholdskoder) { "Ugyldig innholds kode: ${it.innholdskode}" }

        if (it.innholdskode == annetInnholdselement.innholdskode) {
            require(it.beskrivelse != null) {
                "Innhold med innholdskode: ${it.innholdskode} må ha en beskrivelse"
            }
            validerAnnetInnhold(it.beskrivelse)
        }
    }
}
