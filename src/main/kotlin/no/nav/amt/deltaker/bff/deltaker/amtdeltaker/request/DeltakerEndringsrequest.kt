package no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import java.time.LocalDate
import java.util.UUID

sealed interface DeltakerEndringsrequest {
    val endretAv: String
    val endretAvEnhet: String
}

sealed interface DeltakerEndringMedForslag : DeltakerEndringsrequest {
    val forslagId: UUID?
}

data class BakgrunnsinformasjonRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val bakgrunnsinformasjon: String?,
) : DeltakerEndringsrequest

data class InnholdRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val innhold: List<Innhold>,
) : DeltakerEndringsrequest

data class DeltakelsesmengdeRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val deltakelsesprosent: Int?,
    val dagerPerUke: Int?,
    val begrunnelse: String?,
) : DeltakerEndringMedForslag

data class StartdatoRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val startdato: LocalDate?,
    val sluttdato: LocalDate? = null,
) : DeltakerEndringsrequest

data class SluttdatoRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val sluttdato: LocalDate,
    val begrunnelse: String?,
) : DeltakerEndringMedForslag

data class SluttarsakRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val aarsak: DeltakerEndring.Aarsak,
) : DeltakerEndringsrequest

data class ForlengDeltakelseRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val sluttdato: LocalDate,
    val begrunnelse: String?,
) : DeltakerEndringMedForslag

data class IkkeAktuellRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val aarsak: DeltakerEndring.Aarsak,
    val begrunnelse: String?,
) : DeltakerEndringMedForslag

data class AvsluttDeltakelseRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val sluttdato: LocalDate,
    val aarsak: DeltakerEndring.Aarsak,
    val begrunnelse: String?,
) : DeltakerEndringMedForslag

data class ReaktiverDeltakelseRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val begrunnelse: String,
) : DeltakerEndringsrequest
