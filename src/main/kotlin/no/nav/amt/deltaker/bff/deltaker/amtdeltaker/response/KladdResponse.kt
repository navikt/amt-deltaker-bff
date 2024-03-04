package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import java.time.LocalDate
import java.util.UUID

data class KladdResponse(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerlisteId: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val status: DeltakerStatus,
)
