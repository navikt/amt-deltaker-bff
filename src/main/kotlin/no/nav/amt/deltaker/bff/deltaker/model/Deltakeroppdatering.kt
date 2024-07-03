package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDate
import java.util.UUID

data class Deltakeroppdatering(
    val id: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val status: DeltakerStatus,
    val historikk: List<DeltakerHistorikk>,
    val forcedUpdate: Boolean? = false,
)
