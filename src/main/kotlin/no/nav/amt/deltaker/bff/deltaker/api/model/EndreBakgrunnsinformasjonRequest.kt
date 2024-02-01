package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBakgrunnsinformasjon

data class EndreBakgrunnsinformasjonRequest(
    val bakgrunnsinformasjon: String?,
) {
    fun valider() {
        validerBakgrunnsinformasjon(bakgrunnsinformasjon)
    }
}
