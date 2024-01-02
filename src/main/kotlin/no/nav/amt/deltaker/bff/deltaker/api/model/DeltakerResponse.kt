package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerResponse(
    val deltakerId: UUID,
    val deltakerliste: DeltakerlisteDTO,
    val status: DeltakerStatus,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val mal: List<Mal>,
    val sistEndretAv: String,
    val sistEndretAvEnhet: String?,
)

data class DeltakerlisteDTO(
    val deltakerlisteId: UUID,
    val deltakerlisteNavn: String,
    val tiltakstype: Tiltak.Type,
    val arrangorNavn: String,
    val oppstartstype: Deltakerliste.Oppstartstype,
)

data class DeltakerHistorikkDto(
    val endringType: DeltakerEndringType,
    val endring: DeltakerEndring,
    val endretAv: String,
    val endretAvEnhet: String?,
    val endret: LocalDateTime,
)

fun Deltaker.toDeltakerResponse(): DeltakerResponse {
    return DeltakerResponse(
        deltakerId = id,
        deltakerliste = DeltakerlisteDTO(
            deltakerlisteId = deltakerliste.id,
            deltakerlisteNavn = deltakerliste.navn,
            tiltakstype = deltakerliste.tiltak.type,
            arrangorNavn = deltakerliste.arrangor.navn,
            oppstartstype = deltakerliste.getOppstartstype(),
        ),
        status = status,
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        mal = mal,
        sistEndretAv = sistEndretAv,
        sistEndretAvEnhet = sistEndretAvEnhet,
    )
}

private fun DeltakerHistorikk.toDeltakerHistorikkDto() = DeltakerHistorikkDto(
    endringType = endringType,
    endring = endring,
    endretAv = endretAv,
    endretAvEnhet = endretAvEnhet,
    endret = endret,
)
