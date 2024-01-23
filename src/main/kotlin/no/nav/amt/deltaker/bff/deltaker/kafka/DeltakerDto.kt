package no.nav.amt.deltaker.bff.deltaker.kafka

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Mal
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
    val mal: List<Mal>,
    val status: DeltakerStatus,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
)

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
    mal = mal,
    status = status,
    sistEndret = sistEndret,
    opprettet = opprettet,
)
