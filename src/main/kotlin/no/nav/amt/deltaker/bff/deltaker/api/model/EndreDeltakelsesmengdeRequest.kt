package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsent

data class EndreDeltakelsesmengdeRequest(
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
) {
    fun valider() {
        validerDeltakelsesProsent(deltakelsesprosent)
        validerDagerPerUke(dagerPerUke)
    }
}
