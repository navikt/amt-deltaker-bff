package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBakgrunnsinformasjon
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

data class EndreBakgrunnsinformasjonRequest(
    val bakgrunnsinformasjon: String?,
) {
    fun valider(opprinneligDeltaker: Deltaker) {
        validerBakgrunnsinformasjon(bakgrunnsinformasjon)
        require(!opprinneligDeltaker.harSluttet()) {
            "Kan ikke endre bakgrunnsinformasjon for deltaker som har sluttet"
        }
    }
}
