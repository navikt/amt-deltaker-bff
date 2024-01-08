package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.GodkjenningAvNav
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.Endring
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.Endringstype
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteDto
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestData {
    fun randomIdent() = (10_00_00_00_000..31_12_00_99_999).random().toString()

    fun randomNavIdent() = ('A'..'Z').random().toString() + (100_000..999_999).random().toString()

    fun randomEnhetsnummer() = (1000..9999).random().toString()

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
        deltakerliste: Deltakerliste = lagDeltakerliste(),
        startdato: LocalDate? = LocalDate.now().minusMonths(3),
        sluttdato: LocalDate? = LocalDate.now().minusDays(1),
        dagerPerUke: Float? = 5F,
        deltakelsesprosent: Float? = 100F,
        bakgrunnsinformasjon: String? = "Søkes inn fordi...",
        mal: List<Mal> = emptyList(),
        status: DeltakerStatus = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        sistEndretAv: String = randomNavIdent(),
        sistEndretAvEnhet: String = randomEnhetsnummer(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = Deltaker(
        id,
        personident,
        deltakerliste,
        startdato,
        sluttdato,
        dagerPerUke,
        deltakelsesprosent,
        bakgrunnsinformasjon,
        mal,
        status,
        sistEndretAv,
        sistEndretAvEnhet,
        sistEndret,
        opprettet,
    )

    fun lagDeltakerStatus(
        statusType: DeltakerStatus.Type,
        aarsak: DeltakerStatus.Aarsak? = null,
    ) = lagDeltakerStatus(type = statusType, aarsak = aarsak)

    fun lagDeltakerStatus(
        id: UUID = UUID.randomUUID(),
        type: DeltakerStatus.Type = DeltakerStatus.Type.DELTAR,
        aarsak: DeltakerStatus.Aarsak? = null,
        gyldigFra: LocalDateTime = LocalDateTime.now(),
        gyldigTil: LocalDateTime? = null,
        opprettet: LocalDateTime = gyldigFra,
    ) = DeltakerStatus(id, type, aarsak, gyldigFra, gyldigTil, opprettet)

    fun lagDeltakerSamtykke(
        id: UUID = UUID.randomUUID(),
        deltakerVedSamtykke: Deltaker = lagDeltaker(
            status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        ),
        deltakerId: UUID = deltakerVedSamtykke.id,
        godkjent: LocalDateTime? = null,
        gyldigTil: LocalDateTime? = null,
        godkjentAvNav: GodkjenningAvNav? = null,
    ) = DeltakerSamtykke(id, deltakerId, godkjent, gyldigTil, deltakerVedSamtykke, godkjentAvNav)

    fun lagGodkjenningAvNav(
        type: String = "ANNET",
        beskrivelse: String = "Fordi jeg kan",
        godkjentAv: String = randomNavIdent(),
        godkjentAvEnhet: String = randomEnhetsnummer(),
    ) = GodkjenningAvNav(type, beskrivelse, godkjentAv, godkjentAvEnhet)

    fun lagOppdatertDeltaker(
        mal: List<Mal> = emptyList(),
        bakgrunnsinformasjon: String? = "Har vært ...",
        deltakelsesprosent: Float? = 100F,
        dagerPerUke: Float? = 5F,
        godkjentAvNav: GodkjenningAvNav? = null,
        endretAv: String = randomNavIdent(),
        endretAvEnhet: String? = randomEnhetsnummer(),
    ) = OppdatertDeltaker(
        mal,
        bakgrunnsinformasjon,
        deltakelsesprosent,
        dagerPerUke,
        godkjentAvNav,
        endretAv,
        endretAvEnhet,
    )

    fun lagDeltakerEndring(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID,
        endringstype: Endringstype = Endringstype.BAKGRUNNSINFORMASJON,
        endring: Endring = Endring.EndreBakgrunnsinformasjon("Oppdatert bakgrunnsinformasjon"),
        endretAv: String = randomNavIdent(),
        endretAvEnhet: String = randomEnhetsnummer(),
        endret: LocalDateTime = LocalDateTime.now(),
    ) = DeltakerEndring(id, deltakerId, endringstype, endring, endretAv, endretAvEnhet, endret)

    fun lagNavAnsatt(
        id: UUID = UUID.randomUUID(),
        navIdent: String = randomNavIdent(),
        navn: String = "Veileder Veiledersen",
    ) = NavAnsatt(id, navIdent, navn)

    fun lagNavEnhet(
        id: UUID = UUID.randomUUID(),
        enhetsnummer: String = randomEnhetsnummer(),
        navn: String = "NAV Testheim",
    ) = NavEnhet(id, enhetsnummer, navn)

    private fun finnOppstartstype(type: Tiltak.Type) = when (type) {
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }
}
