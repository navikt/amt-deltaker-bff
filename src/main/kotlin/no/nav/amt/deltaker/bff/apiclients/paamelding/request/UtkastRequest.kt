package no.nav.amt.deltaker.bff.apiclients.paamelding.request

import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold

data class UtkastRequest(
    val deltakelsesinnhold: Deltakelsesinnhold,
    val bakgrunnsinformasjon: String?,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val endretAv: String,
    val endretAvEnhet: String,
    val godkjentAvNav: Boolean,
) {
    companion object {
        fun fromUtkast(utkast: Utkast): UtkastRequest = with(utkast.pamelding) {
            UtkastRequest(
                deltakelsesinnhold = deltakelsesinnhold,
                bakgrunnsinformasjon = bakgrunnsinformasjon,
                deltakelsesprosent = deltakelsesprosent,
                dagerPerUke = dagerPerUke,
                endretAv = endretAv,
                endretAvEnhet = endretAvEnhet,
                godkjentAvNav = utkast.godkjentAvNav,
            )
        }
    }
}
