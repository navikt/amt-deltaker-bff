package no.nav.amt.deltaker.bff.deltaker.api.utils

import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import java.time.LocalDate

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

fun validerAnnetInnhold(tekst: String?) {
    require(tekst != null && tekst != "") {
        "Innhold med innholdskode: ${annetInnholdselement.innholdskode} må ha en beskrivelse"
    }
    require(tekst.length <= MAX_ANNET_INNHOLD_LENGDE) {
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

fun validerDeltakerKanEndres(opprinneligDeltaker: Deltaker) {
    if (opprinneligDeltaker.harSluttet()) {
        require(opprinneligDeltaker.kanEndres) {
            "Kan ikke endre avsluttet deltakelse når det finnes aktiv deltakelse på samme tiltak"
        }
        require(opprinneligDeltaker.harSluttetForMindreEnnToMndSiden()) {
            "Kan ikke endre deltaker som fikk avsluttende status for mer enn to måneder siden"
        }
    }
}

fun validerSluttdatoForDeltaker(
    sluttdato: LocalDate,
    startdato: LocalDate?,
    opprinneligDeltaker: Deltaker,
) {
    require(opprinneligDeltaker.deltakerliste.sluttDato == null || !sluttdato.isAfter(opprinneligDeltaker.deltakerliste.sluttDato)) {
        "Sluttdato kan ikke være senere enn deltakerlistens sluttdato"
    }
    require(startdato == null || !sluttdato.isBefore(startdato)) {
        "Sluttdato må være etter startdato"
    }
}

fun validerDeltakelsesinnhold(innhold: List<InnholdDto>, tiltaksinnhold: DeltakerRegistreringInnhold?) {
    validerInnhold(innhold, tiltaksinnhold) { innholdskoder ->
        require(innhold.isNotEmpty()) { "For et tiltak med innholdselementer må det velges minst ett" }

        innhold.forEach {
            require(it.innholdskode in innholdskoder) { "Ugyldig innholds kode: ${it.innholdskode}" }

            if (it.innholdskode == annetInnholdselement.innholdskode) {
                validerAnnetInnhold(it.beskrivelse)
            } else {
                require(it.beskrivelse == null) {
                    "Innhold med innholdskode: ${it.innholdskode} kan ikke ha en beskrivelse"
                }
            }
        }
    }
}

fun validerKladdInnhold(innhold: List<InnholdDto>, tiltaksinnhold: DeltakerRegistreringInnhold?) {
    validerInnhold(innhold, tiltaksinnhold) { innholdskoder ->
        innhold.forEach {
            require(it.innholdskode in innholdskoder) { "Ugyldig innholds kode: ${it.innholdskode}" }

            if (it.innholdskode != annetInnholdselement.innholdskode) {
                require(it.beskrivelse == null) {
                    "Innhold med innholdskode: ${it.innholdskode} kan ikke ha en beskrivelse"
                }
            }
        }
    }
}

private fun validerInnhold(
    innhold: List<InnholdDto>,
    tiltaksinnhold: DeltakerRegistreringInnhold?,
    valider: (innholdskoder: List<String>) -> Unit,
) {
    val innholdskoder = tiltaksinnhold?.innholdselementer?.map { it.innholdskode }

    if (innholdskoder.isNullOrEmpty()) {
        require(innhold.isEmpty()) { "Et tiltak uten innholdselementer kan ikke ha noe innhold" }
    } else {
        valider(innholdskoder)
    }
}
