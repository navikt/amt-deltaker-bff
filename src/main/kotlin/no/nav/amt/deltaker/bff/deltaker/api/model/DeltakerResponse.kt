package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import java.time.LocalDate
import java.util.UUID

data class DeltakerResponse(
    val deltakerId: UUID,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val deltakerliste: DeltakerlisteDto,
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

data class DeltakerlisteDto(
    val deltakerlisteId: UUID,
    val deltakerlisteNavn: String,
    val tiltakstype: Tiltak.Type,
    val arrangorNavn: String,
    val oppstartstype: Deltakerliste.Oppstartstype,
)

fun Deltaker.toDeltakerResponse(): DeltakerResponse {
    return DeltakerResponse(
        deltakerId = id,
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        deltakerliste = DeltakerlisteDto(
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
