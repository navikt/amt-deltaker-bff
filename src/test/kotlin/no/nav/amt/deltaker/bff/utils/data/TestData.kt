package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltaker.Deltaker
import no.nav.amt.deltaker.bff.deltaker.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestData {
    fun randomIdent() = (10_00_00_00_000..31_12_00_99_999).random().toString()

    fun randomNavIdent() = ('A'..'Z').random().toString() + (100_000..999_999).random().toString()

    fun randomEnhetId() = (1000..9999).random().toString()

    fun randomOrgnr() = (900_000_000..999_999_998).random().toString()

    fun lagArrangor(
        id: UUID = UUID.randomUUID(),
        navn: String = "Arrangor 1",
        organisasjonsnummer: String = randomOrgnr(),
        overordnetArrangorId: UUID? = null,
    ) = Arrangor(id, navn, organisasjonsnummer, overordnetArrangorId)

    fun lagDeltakerliste(
        id: UUID = UUID.randomUUID(),
        arrangor: Arrangor = lagArrangor(),
        tiltak: Tiltak = lagTiltak(),
        navn: String = "Test Deltakerliste ${tiltak.type}",
        status: Deltakerliste.Status = Deltakerliste.Status.GJENNOMFORES,
        startDato: LocalDate = LocalDate.now().minusMonths(1),
        sluttDato: LocalDate? = LocalDate.now().plusYears(1),
        oppstart: Deltakerliste.Oppstartstype? = finnOppstartstype(tiltak.type),
    ) = Deltakerliste(id, tiltak, navn, status, startDato, sluttDato, oppstart, arrangor)

    fun lagTiltak(
        type: Tiltak.Type = Tiltak.Type.entries.random(),
        navn: String = "Test tiltak $type",
    ) = Tiltak(navn, type)

    fun lagDeltakerlisteDto(
        arrangor: Arrangor = lagArrangor(),
        deltakerliste: Deltakerliste = lagDeltakerliste(arrangor = arrangor),
    ) = DeltakerlisteDto(
        id = deltakerliste.id,
        tiltakstype = DeltakerlisteDto.Tiltakstype(
            deltakerliste.tiltak.navn,
            deltakerliste.tiltak.type.name,
        ),
        navn = deltakerliste.navn,
        startDato = deltakerliste.startDato,
        sluttDato = deltakerliste.sluttDato,
        status = deltakerliste.status.name,
        virksomhetsnummer = arrangor.organisasjonsnummer,
        oppstart = deltakerliste.oppstart,
    )

    fun lagDeltaker(
        id: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        deltakerlisteId: UUID = UUID.randomUUID(),
        startdato: LocalDate? = LocalDate.now().minusMonths(3),
        sluttdato: LocalDate? = LocalDate.now().minusDays(1),
        dagerPerUke: Float? = 5F,
        deltakelsesprosent: Float? = 100F,
        bakgrunnsinformasjon: String? = "Søkes inn fordi...",
        mal: List<Mal> = emptyList(),
        status: DeltakerStatus = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        sistEndretAv: String = randomNavIdent(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = Deltaker(
        id,
        personident,
        deltakerlisteId,
        startdato,
        sluttdato,
        dagerPerUke,
        deltakelsesprosent,
        bakgrunnsinformasjon,
        mal,
        status,
        sistEndretAv,
        sistEndret,
        opprettet,
    )

    fun lagDeltakerStatus(
        id: UUID = UUID.randomUUID(),
        type: DeltakerStatus.Type = DeltakerStatus.Type.DELTAR,
        aarsak: DeltakerStatus.Aarsak? = null,
        gyldigFra: LocalDateTime = LocalDateTime.now(),
        gyldigTil: LocalDateTime? = null,
        opprettet: LocalDateTime = gyldigFra,
    ) = DeltakerStatus(id, type, aarsak, gyldigFra, gyldigTil, opprettet)

    private fun finnOppstartstype(type: Tiltak.Type) = when (type) {
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }
}
