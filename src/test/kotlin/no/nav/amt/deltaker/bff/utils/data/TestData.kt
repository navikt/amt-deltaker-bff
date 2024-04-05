package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltaker.navbruker.Adressebeskyttelse
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltaker.toDeltakerVedVedtak
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteDto
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
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

    fun input(n: Int) = (1..n).map { ('a'..'z').random() }.joinToString("")

    fun lagArrangor(
        id: UUID = UUID.randomUUID(),
        navn: String = "Arrangor 1",
        organisasjonsnummer: String = randomOrgnr(),
        overordnetArrangorId: UUID? = null,
    ) = Arrangor(id, navn, organisasjonsnummer, overordnetArrangorId)

    fun lagDeltakerliste(
        id: UUID = UUID.randomUUID(),
        overordnetArrangor: Arrangor? = null,
        arrangor: Arrangor = lagArrangor(overordnetArrangorId = overordnetArrangor?.id),
        tiltak: Tiltakstype = lagTiltakstype(),
        navn: String = "Test Deltakerliste ${tiltak.type}",
        status: Deltakerliste.Status = Deltakerliste.Status.GJENNOMFORES,
        startDato: LocalDate = LocalDate.now().minusMonths(1),
        sluttDato: LocalDate? = LocalDate.now().plusYears(1),
        oppstart: Deltakerliste.Oppstartstype? = finnOppstartstype(tiltak.type),
    ) = Deltakerliste(
        id,
        tiltak,
        navn,
        status,
        startDato,
        sluttDato,
        oppstart,
        Deltakerliste.Arrangor(arrangor, overordnetArrangor?.navn),
    )

    private val tiltakstypeCache = mutableMapOf<Tiltak.Type, Tiltakstype>()

    fun lagTiltakstype(
        id: UUID = UUID.randomUUID(),
        type: Tiltak.Type = Tiltak.Type.entries.random(),
        navn: String = "Test tiltak $type",
        innhold: DeltakerRegistreringInnhold? = lagDeltakerRegistreringInnhold(),
    ): Tiltakstype {
        val tiltak = tiltakstypeCache[type] ?: Tiltakstype(id, navn, type, innhold)
        val nyttTiltak = tiltak.copy(navn = navn, innhold = innhold)
        tiltakstypeCache[tiltak.type] = nyttTiltak

        return nyttTiltak
    }

    fun lagDeltakerRegistreringInnhold(
        innholdselementer: List<Innholdselement> = listOf(Innholdselement("Tekst", "kode")),
        ledetekst: String = "Beskrivelse av tilaket",
    ) = DeltakerRegistreringInnhold(innholdselementer, ledetekst)

    fun lagDeltakerlisteDto(arrangor: Arrangor = lagArrangor(), deltakerliste: Deltakerliste = lagDeltakerliste(arrangor = arrangor)) =
        DeltakerlisteDto(
            id = deltakerliste.id,
            tiltakstype = DeltakerlisteDto.TiltakstypeDto(
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

    fun lagDeltakerKladd(
        id: UUID = UUID.randomUUID(),
        navBruker: NavBruker = lagNavBruker(),
        deltakerliste: Deltakerliste = lagDeltakerliste(),
        sistEndret: LocalDateTime = LocalDateTime.now(),
    ) = lagDeltaker(
        id = id,
        navBruker = navBruker,
        deltakerliste = deltakerliste,
        startdato = null,
        sluttdato = null,
        dagerPerUke = null,
        deltakelsesprosent = null,
        bakgrunnsinformasjon = null,
        innhold = emptyList(),
        status = lagDeltakerStatus(DeltakerStatus.Type.KLADD),
        historikk = false,
        sistEndret = sistEndret,
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
        innhold: List<Innhold> = deltakerliste.tiltak.innhold?.innholdselementer?.map { it.toInnhold() } ?: emptyList(),
        status: DeltakerStatus = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        historikk: Boolean = true,
        kanEndres: Boolean = true,
        sistEndret: LocalDateTime = LocalDateTime.now(),
    ): Deltaker {
        val deltaker = Deltaker(
            id,
            navBruker,
            deltakerliste,
            startdato,
            sluttdato,
            dagerPerUke,
            deltakelsesprosent,
            bakgrunnsinformasjon,
            innhold,
            status,
            emptyList(),
            kanEndres,
            sistEndret,
        )

        return if (historikk) {
            deltaker.copy(historikk = lagDeltakerHistorikk(deltaker))
        } else {
            deltaker
        }
    }

    private fun lagDeltakerHistorikk(deltaker: Deltaker): List<DeltakerHistorikk> {
        val vedtak = lagVedtak(deltakerVedVedtak = deltaker, fattet = LocalDateTime.now())
        return listOf(DeltakerHistorikk.Vedtak(vedtak))
    }

    fun lagDeltakerStatus(type: DeltakerStatus.Type, aarsak: DeltakerStatus.Aarsak) =
        lagDeltakerStatus(type, aarsak.type, aarsak.beskrivelse)

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

    fun lagVedtak(
        id: UUID = UUID.randomUUID(),
        deltakerVedVedtak: Deltaker = lagDeltaker(
            status = lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
        ),
        deltakerId: UUID = deltakerVedVedtak.id,
        fattet: LocalDateTime? = null,
        gyldigTil: LocalDateTime? = null,
        fattetAvNav: Boolean = false,
        opprettet: LocalDateTime = LocalDateTime.now(),
        opprettetAv: UUID = UUID.randomUUID(),
        opprettetAvEnhet: UUID = UUID.randomUUID(),
        sistEndret: LocalDateTime = opprettet,
        sistEndretAv: UUID = opprettetAv,
        sistEndretAvEnhet: UUID = opprettetAvEnhet,
    ) = Vedtak(
        id,
        deltakerId,
        fattet,
        gyldigTil,
        deltakerVedVedtak.toDeltakerVedVedtak(),
        fattetAvNav,
        opprettet,
        opprettetAv,
        opprettetAvEnhet,
        sistEndret,
        sistEndretAv,
        sistEndretAvEnhet,
    )

    fun lagDeltakerEndring(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        endring: DeltakerEndring.Endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("Oppdatert bakgrunnsinformasjon"),
        endretAv: UUID = UUID.randomUUID(),
        endretAvEnhet: UUID = UUID.randomUUID(),
        endret: LocalDateTime = LocalDateTime.now(),
    ) = DeltakerEndring(id, deltakerId, endring, endretAv, endretAvEnhet, endret)

    fun lagNavAnsatt(
        id: UUID = UUID.randomUUID(),
        navIdent: String = randomNavIdent(),
        navn: String = "Veileder Veiledersen",
    ) = NavAnsatt(id, navIdent, navn)

    private val navEnhetCache = mutableMapOf<String, NavEnhet>()

    fun lagNavEnhet(
        id: UUID = UUID.randomUUID(),
        enhetsnummer: String = randomEnhetsnummer(),
        navn: String = "NAV Testheim",
    ): NavEnhet {
        val enhet = navEnhetCache[enhetsnummer] ?: NavEnhet(id, enhetsnummer, navn)
        navEnhetCache[enhetsnummer] = enhet
        return enhet
    }

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
        adressebeskyttelse: Adressebeskyttelse? = null,
    ) = NavBruker(personId, personident, fornavn, mellomnavn, etternavn, adressebeskyttelse)

    private fun finnOppstartstype(type: Tiltak.Type) = when (type) {
        Tiltak.Type.JOBBK,
        Tiltak.Type.GRUPPEAMO,
        Tiltak.Type.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }

    fun lagNavAnsatteForDeltaker(deltaker: Deltaker) = listOfNotNull(
        deltaker.vedtaksinformasjon?.sistEndretAv,
        deltaker.vedtaksinformasjon?.opprettetAv,
    ).distinct().map { lagNavAnsatt(id = it) }

    fun lagNavAnsatteForHistorikk(historikk: List<DeltakerHistorikk>) = historikk.flatMap {
        when (it) {
            is DeltakerHistorikk.Endring -> {
                listOf(it.endring.endretAv)
            }

            is DeltakerHistorikk.Vedtak -> {
                listOfNotNull(
                    it.vedtak.sistEndretAv,
                    it.vedtak.opprettetAv,
                )
            }
        }
    }.distinct().map { lagNavAnsatt(id = it) }

    fun leggTilHistorikk(
        deltaker: Deltaker,
        antallVedtak: Int = 1,
        antallEndringer: Int = 1,
    ): Deltaker {
        val vedtak = (1..antallVedtak).map {
            val fattet = it == antallVedtak
            lagVedtak(
                deltakerVedVedtak = deltaker,
                fattet = if (fattet) LocalDateTime.now() else null,
                gyldigTil = if (fattet) null else LocalDateTime.now(),
                fattetAvNav = fattet,
            )
        }

        val endringer = (1..antallEndringer).map { lagDeltakerEndring(deltakerId = deltaker.id) }

        return deltaker.copy(
            historikk = vedtak.map { DeltakerHistorikk.Vedtak(it) } + endringer.map { DeltakerHistorikk.Endring(it) },
        )
    }

    fun leggTilHistorikk(
        deltaker: Deltaker,
        vedtak: List<Vedtak> = emptyList(),
        endringer: List<DeltakerEndring> = emptyList(),
    ) = deltaker.copy(
        historikk = deltaker.historikk
            .plus(vedtak.map { DeltakerHistorikk.Vedtak(it) })
            .plus(endringer.map { DeltakerHistorikk.Endring(it) }),
    )
}

fun Deltaker.endre(deltakerEndring: DeltakerEndring): Deltaker {
    val deltaker = when (val endring = deltakerEndring.endring) {
        is DeltakerEndring.Endring.AvsluttDeltakelse -> this.copy(
            sluttdato = endring.sluttdato,
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = endring.aarsak.toStatusAarsak().type,
                beskrivelse = endring.aarsak.beskrivelse,
            ),
        )

        is DeltakerEndring.Endring.EndreBakgrunnsinformasjon ->
            this.copy(bakgrunnsinformasjon = endring.bakgrunnsinformasjon)

        is DeltakerEndring.Endring.EndreDeltakelsesmengde -> this.copy(
            dagerPerUke = endring.dagerPerUke,
            deltakelsesprosent = endring.deltakelsesprosent,
        )

        is DeltakerEndring.Endring.EndreInnhold -> this.copy(innhold = endring.innhold)
        is DeltakerEndring.Endring.EndreSluttarsak ->
            this.copy(status = this.status.copy(aarsak = endring.aarsak.toStatusAarsak()))

        is DeltakerEndring.Endring.EndreSluttdato -> this.copy(sluttdato = endring.sluttdato)
        is DeltakerEndring.Endring.EndreStartdato -> this.copy(startdato = endring.startdato, sluttdato = endring.sluttdato)
        is DeltakerEndring.Endring.ForlengDeltakelse -> this.copy(sluttdato = endring.sluttdato)
        is DeltakerEndring.Endring.IkkeAktuell -> this.copy(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.IKKE_AKTUELL,
                aarsak = endring.aarsak.toStatusAarsak().type,
                beskrivelse = endring.aarsak.beskrivelse,
            ),
        )
    }
    return deltaker.copy(historikk = this.historikk.plus(DeltakerHistorikk.Endring(deltakerEndring)))
}

fun DeltakerEndring.Aarsak.toStatusAarsak() = DeltakerStatus.Aarsak(
    type = DeltakerStatus.Aarsak.Type.valueOf(this.type.name),
    beskrivelse = this.beskrivelse,
)
