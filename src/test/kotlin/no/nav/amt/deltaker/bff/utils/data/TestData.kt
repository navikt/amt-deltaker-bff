package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.GodkjentAvNav
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Mal
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteDto
import no.nav.amt.deltaker.bff.endringsmelding.Endringsmelding
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
        navBruker: NavBruker = lagNavBruker(),
        deltakerliste: Deltakerliste = lagDeltakerliste(),
        startdato: LocalDate? = LocalDate.now().minusMonths(3),
        sluttdato: LocalDate? = LocalDate.now().minusDays(1),
        dagerPerUke: Float? = 5F,
        deltakelsesprosent: Float? = 100F,
        bakgrunnsinformasjon: String? = "Søkes inn fordi...",
        mal: List<Mal> = emptyList(),
        status: DeltakerStatus = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        samtykke: Deltaker.Samtykke = lagDeltakerSamtykkeData(godkjent = LocalDateTime.now().minusMonths(4)),
        sistEndretAv: String = randomNavIdent(),
        sistEndretAvEnhet: String = randomEnhetsnummer(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = Deltaker(
        id,
        navBruker,
        deltakerliste,
        startdato,
        sluttdato,
        dagerPerUke,
        deltakelsesprosent,
        bakgrunnsinformasjon,
        mal,
        status,
        samtykke,
        sistEndretAv,
        sistEndretAvEnhet,
        sistEndret,
        opprettet,
    )

    fun lagDeltakerStatus(
        type: DeltakerStatus.Type,
        aarsak: DeltakerStatus.Aarsak,
    ) = lagDeltakerStatus(type, aarsak.type, aarsak.beskrivelse)

    fun lagDeltakerStatus(
        statusType: DeltakerStatus.Type,
        aarsak: DeltakerStatus.Aarsak.Type? = null,
        beskrivelse: String? = null,
    ) = lagDeltakerStatus(type = statusType, aarsak = aarsak, beskrivelse = beskrivelse)

    fun lagDeltakerStatus(
        id: UUID = UUID.randomUUID(),
        type: DeltakerStatus.Type = DeltakerStatus.Type.DELTAR,
        aarsak: DeltakerStatus.Aarsak.Type? = null,
        beskrivelse: String? = null,
        gyldigFra: LocalDateTime = LocalDateTime.now(),
        gyldigTil: LocalDateTime? = null,
        opprettet: LocalDateTime = gyldigFra,
    ) = DeltakerStatus(
        id,
        type,
        aarsak?.let { DeltakerStatus.Aarsak(it, beskrivelse) },
        gyldigFra,
        gyldigTil,
        opprettet,
    )

    fun lagDeltakerSamtykke(
        id: UUID = UUID.randomUUID(),
        deltakerVedSamtykke: Deltaker = lagDeltaker(
            status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        ),
        deltakerId: UUID = deltakerVedSamtykke.id,
        godkjent: LocalDateTime? = null,
        gyldigTil: LocalDateTime? = null,
        godkjentAvNav: GodkjentAvNav? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        opprettetAv: String = randomNavIdent(),
        opprettetAvEnhet: String? = randomEnhetsnummer(),
        sistEndret: LocalDateTime = opprettet,
        sistEndretAv: String = opprettetAv,
        sistEndretAvEnhet: String? = opprettetAvEnhet,
    ) = DeltakerSamtykke(
        id,
        deltakerId,
        godkjent,
        gyldigTil,
        deltakerVedSamtykke,
        godkjentAvNav,
        opprettet,
        opprettetAv,
        opprettetAvEnhet,
        sistEndret,
        sistEndretAv,
        sistEndretAvEnhet,
    )

    fun lagDeltakerSamtykkeData(
        godkjent: LocalDateTime? = LocalDateTime.now(),
        godkjentAvNav: GodkjentAvNav? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        opprettetAv: String = randomNavIdent(),
        sistEndret: LocalDateTime = opprettet,
        sistEndretAv: String = opprettetAv,
    ) = Deltaker.Samtykke(godkjent, godkjentAvNav, opprettet, opprettetAv, sistEndret, sistEndretAv)

    fun lagGodkjenningAvNav(
        godkjentAv: String = randomNavIdent(),
        godkjentAvEnhet: String = randomEnhetsnummer(),
    ) = GodkjentAvNav(godkjentAv, godkjentAvEnhet)

    fun lagPamelding(
        deltaker: Deltaker,
        mal: List<Mal>? = null,
        bakgrunnsinformasjon: String? = null,
        deltakelsesprosent: Float? = null,
        dagerPerUke: Float? = null,
        endretAv: String? = null,
        endretAvEnhet: String? = null,
    ) = lagPamelding(
        mal = mal ?: deltaker.mal,
        bakgrunnsinformasjon = bakgrunnsinformasjon ?: deltaker.bakgrunnsinformasjon,
        deltakelsesprosent = deltakelsesprosent ?: deltaker.deltakelsesprosent,
        dagerPerUke = dagerPerUke ?: deltaker.dagerPerUke,
        endretAv = endretAv ?: deltaker.sistEndretAv,
        endretAvEnhet = endretAvEnhet ?: deltaker.sistEndretAvEnhet,
    )

    fun lagPamelding(
        mal: List<Mal> = emptyList(),
        bakgrunnsinformasjon: String? = "Har vært ...",
        deltakelsesprosent: Float? = 100F,
        dagerPerUke: Float? = 5F,
        endretAv: String = randomNavIdent(),
        endretAvEnhet: String? = randomEnhetsnummer(),
    ) = Pamelding(
        mal,
        bakgrunnsinformasjon,
        deltakelsesprosent,
        dagerPerUke,
        endretAv,
        endretAvEnhet,
    )

    fun lagKladd(
        opprinneligDeltaker: Deltaker = lagDeltaker(
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            mal = emptyList(),
            status = lagDeltakerStatus(DeltakerStatus.Type.KLADD),
        ),
        pamelding: Pamelding = lagPamelding(),
    ) = Kladd(opprinneligDeltaker, pamelding)

    fun lagUtkast(
        opprinneligDeltaker: Deltaker = lagDeltaker(
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            mal = emptyList(),
            status = lagDeltakerStatus(DeltakerStatus.Type.KLADD),
        ),
        pamelding: Pamelding = lagPamelding(opprinneligDeltaker),
        godkjentAvNav: GodkjentAvNav? = null,
    ) = Utkast(opprinneligDeltaker, pamelding, godkjentAvNav)

    fun lagDeltakerEndring(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        endringstype: DeltakerEndring.Endringstype = DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
        endring: DeltakerEndring.Endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("Oppdatert bakgrunnsinformasjon"),
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

    fun lagEndringsmelding(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        utfortAvNavAnsattId: UUID? = null,
        utfortTidspunkt: LocalDateTime? = null,
        opprettetAvArrangorAnsattId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        status: Endringsmelding.Status = Endringsmelding.Status.AKTIV,
        innhold: Endringsmelding.Innhold = Endringsmelding.Innhold.LeggTilOppstartsdatoInnhold(
            LocalDate.now().plusDays(2),
        ),
        type: Endringsmelding.Type = Endringsmelding.Type.LEGG_TIL_OPPSTARTSDATO,
    ) = Endringsmelding(
        id,
        deltakerId,
        utfortAvNavAnsattId,
        utfortTidspunkt,
        opprettetAvArrangorAnsattId,
        opprettet,
        status,
        innhold,
        type,
    )

    fun lagNavBruker(
        personId: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        fornavn: String = "Fornavn",
        mellomnavn: String? = "Mellomnavn",
        etternavn: String = "Etternavn",
    ) = NavBruker(personId, personident, fornavn, mellomnavn, etternavn)

    private fun finnOppstartstype(type: Tiltak.Type) = when (type) {
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }
}
