package no.nav.amt.deltaker.bff.deltaker.kafka

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDto(
    val id: UUID,
    val personId: UUID,
    val personident: String,
    val deltakerlisteId: UUID,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: DeltakelsesinnholdDto?,
    val status: DeltakerStatus,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
) {
    data class DeltakelsesinnholdDto(
        val ledetekst: String,
        val innhold: List<InnholdDto>,
    )
}

fun Deltaker.toDto() = DeltakerDto(
    id = id,
    personId = navBruker.personId,
    personident = navBruker.personident,
    deltakerlisteId = deltakerliste.id,
    startdato = startdato,
    sluttdato = sluttdato,
    dagerPerUke = dagerPerUke,
    deltakelsesprosent = deltakelsesprosent,
    bakgrunnsinformasjon = bakgrunnsinformasjon,
    innhold = deltakerliste.tiltak.innhold?.let {
        DeltakerDto.DeltakelsesinnholdDto(
            it.ledetekst,
            innhold.toDto(),
        )
    },
    status = status,
    sistEndret = sistEndret,
    opprettet = opprettet,
)

data class InnholdDto(
    val tekst: String,
    val innholdskode: String,
    val beskrivelse: String?,
)

fun List<Innhold>.toDto() = this.map { InnholdDto(it.tekst, it.innholdskode, it.beskrivelse) }
