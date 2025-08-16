package no.nav.amt.deltaker.bff.apiclients.paamelding.response

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.person.NavBruker
import java.time.LocalDate
import java.util.UUID

data class OpprettKladdResponse(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerlisteId: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: Deltakelsesinnhold,
    val status: DeltakerStatus,
) {
    companion object {
        fun fromDeltaker(deltaker: Deltaker) = with(deltaker) {
            OpprettKladdResponse(
                id = id,
                navBruker = navBruker,
                deltakerlisteId = deltakerliste.id,
                startdato = startdato,
                sluttdato = sluttdato,
                dagerPerUke = dagerPerUke,
                deltakelsesprosent = deltakelsesprosent,
                bakgrunnsinformasjon = bakgrunnsinformasjon,
                deltakelsesinnhold = deltakelsesinnhold!!,
                status = status,
            )
        }
    }
}
