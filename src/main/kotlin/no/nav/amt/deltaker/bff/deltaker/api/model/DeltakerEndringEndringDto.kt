package no.nav.amt.deltaker.bff.deltaker.api.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.deltaker.DeltakerEndring.Aarsak
import no.nav.amt.lib.models.deltaker.Innhold
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DeltakerEndringEndringDto {
    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : DeltakerEndringEndringDto()

    data class EndreInnhold(
        val ledetekst: String?,
        val innhold: List<Innhold>,
    ) : DeltakerEndringEndringDto()

    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Float?,
        val dagerPerUke: Float?,
        val gyldigFra: LocalDate?,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate?,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class EndreSluttdato(
        val sluttdato: LocalDate,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class IkkeAktuell(
        val aarsak: Aarsak,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class AvsluttDeltakelse(
        val aarsak: Aarsak?,
        val sluttdato: LocalDate,
        val begrunnelse: String?,
        val harFullfort: Boolean,
        val oppstartstype: Deltakerliste.Oppstartstype,
    ) : DeltakerEndringEndringDto()

    data class EndreAvslutning(
        val aarsak: Aarsak?,
        val begrunnelse: String?,
        val harFullfort: Boolean,
    ) : DeltakerEndringEndringDto()

    data class EndreSluttarsak(
        val aarsak: Aarsak,
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()

    data class ReaktiverDeltakelse(
        val reaktivertDato: LocalDate,
        val begrunnelse: String,
    ) : DeltakerEndringEndringDto()

    data class FjernOppstartsdato(
        val begrunnelse: String?,
    ) : DeltakerEndringEndringDto()
}
