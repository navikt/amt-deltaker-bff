package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.api.utils.validerBegrunnelse
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsent
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakelsesProsentOgDagerPerUke
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanEndres
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import java.util.UUID

data class EndreDeltakelsesmengdeRequest(
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
    val begrunnelse: String?,
    override val forslagId: UUID?,
) : EndringsforslagRequest {
    override fun valider(deltaker: Deltaker) {
        validerDeltakelsesProsent(deltakelsesprosent)
        validerDagerPerUke(dagerPerUke)
        validerDeltakelsesProsentOgDagerPerUke(nyProsnet = deltakelsesprosent, nyDagerPerUke = dagerPerUke, deltaker)
        validerDeltakerKanEndres(deltaker)
        validerBegrunnelse(begrunnelse)
    }
}
