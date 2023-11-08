package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.application.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.application.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.application.deltakerliste.kafka.DeltakerlisteDto
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import java.time.LocalDate
import java.util.UUID

object TestData {
    fun randomIdent() = (10_00_19_00_00_000..31_12_20_99_99_999).random().toString()

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
        arrangorId: UUID = UUID.randomUUID(),
        tiltak: Tiltak = lagTiltak(),
        navn: String = "Test Deltakerliste ${tiltak.type}",
        status: Deltakerliste.Status = Deltakerliste.Status.GJENNOMFORES,
        startDato: LocalDate = LocalDate.now().minusMonths(1),
        sluttDato: LocalDate? = LocalDate.now().plusYears(1),
        oppstart: Deltakerliste.Oppstartstype? = finnOppstartstype(tiltak.type),
    ) = Deltakerliste(id, arrangorId, tiltak, navn, status, startDato, sluttDato, oppstart)

    fun lagTiltak(
        type: Tiltak.Type = Tiltak.Type.entries.random(),
        navn: String = "Test tiltak $type",
    ) = Tiltak(navn, type)

    fun lagDeltakerlisteDto(
        deltakerliste: Deltakerliste = lagDeltakerliste(),
        arrangor: Arrangor = lagArrangor(deltakerliste.arrangorId),
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

    private fun finnOppstartstype(type: Tiltak.Type) = when (type) {
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }
}
