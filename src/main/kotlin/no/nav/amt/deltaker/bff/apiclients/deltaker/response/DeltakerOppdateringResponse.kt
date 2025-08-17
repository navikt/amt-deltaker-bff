package no.nav.amt.deltaker.bff.apiclients.deltaker.response

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerOppdateringResponse(
    val id: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val status: DeltakerStatus,
    val historikk: List<DeltakerHistorikk>,
    val sistEndret: LocalDateTime = LocalDateTime.now(),
    val erManueltDeltMedArrangor: Boolean,
    val feilkode: DeltakerOppdateringFeilkode?,
) {
    fun toDeltakerOppdatering() = Deltakeroppdatering(
        id = id,
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        deltakelsesinnhold = deltakelsesinnhold,
        status = status,
        historikk = historikk,
        sistEndret = sistEndret,
        erManueltDeltMedArrangor = erManueltDeltMedArrangor,
    )

    companion object {
        fun fromDeltaker(deltaker: Deltaker, feilkode: DeltakerOppdateringFeilkode? = null) = with(deltaker) {
            DeltakerOppdateringResponse(
                id = id,
                startdato = startdato,
                sluttdato = sluttdato,
                dagerPerUke = dagerPerUke,
                deltakelsesprosent = deltakelsesprosent,
                bakgrunnsinformasjon = bakgrunnsinformasjon,
                deltakelsesinnhold = deltakelsesinnhold,
                status = status,
                historikk = historikk,
                erManueltDeltMedArrangor = erManueltDeltMedArrangor,
                sistEndret = sistEndret,
                feilkode = feilkode,
            )
        }
    }
}
