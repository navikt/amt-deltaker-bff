package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
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
    val historikk: List<DeltakerHistorikkDto>,
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
