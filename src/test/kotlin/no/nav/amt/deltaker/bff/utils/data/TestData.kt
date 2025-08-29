package no.nav.amt.deltaker.bff.utils.data

import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.toDeltakerVedVedtak
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteDto
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.toInnhold
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.deltaker.bff.tiltakskoordinator.toTiltakskoordinatorsDeltaker
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import no.nav.amt.lib.models.deltaker.Arrangor
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.DeltakerVedImport
import no.nav.amt.lib.models.deltaker.ImportertFraArena
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.deltaker.Vedtak
import no.nav.amt.lib.models.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.models.hendelse.Hendelse
import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
import no.nav.amt.lib.models.hendelse.HendelseDeltaker
import no.nav.amt.lib.models.hendelse.HendelseType
import no.nav.amt.lib.models.person.NavAnsatt
import no.nav.amt.lib.models.person.NavBruker
import no.nav.amt.lib.models.person.NavEnhet
import no.nav.amt.lib.models.person.Oppfolgingsperiode
import no.nav.amt.lib.models.person.address.Adresse
import no.nav.amt.lib.models.person.address.Adressebeskyttelse
import no.nav.amt.lib.models.person.address.Bostedsadresse
import no.nav.amt.lib.models.person.address.Kontaktadresse
import no.nav.amt.lib.models.person.address.Matrikkeladresse
import no.nav.amt.lib.models.person.address.Vegadresse
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
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
        navn: String = "Test Deltakerliste ${tiltak.arenaKode}",
        status: Deltakerliste.Status = Deltakerliste.Status.GJENNOMFORES,
        startDato: LocalDate = LocalDate.now().minusMonths(1),
        sluttDato: LocalDate? = LocalDate.now().plusYears(1),
        oppstart: Deltakerliste.Oppstartstype = finnOppstartstype(tiltak.arenaKode),
        apentForPamelding: Boolean = true,
        antallPlasser: Int = 42,
    ) = Deltakerliste(
        id,
        tiltak,
        navn,
        status,
        startDato,
        sluttDato,
        oppstart,
        Deltakerliste.Arrangor(arrangor, overordnetArrangor?.navn),
        apentForPamelding,
        antallPlasser,
    )

    private val tiltakstypeCache = mutableMapOf<Tiltakstype.Tiltakskode, Tiltakstype>()

    fun lagTiltakstype(
        id: UUID = UUID.randomUUID(),
        tiltakskode: Tiltakstype.Tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arenaKode: Tiltakstype.ArenaKode = tiltakskode.toArenaKode(),
        navn: String = "Test tiltak $arenaKode",
        innsatsgrupper: Set<Innsatsgruppe> = setOf(Innsatsgruppe.STANDARD_INNSATS),
        innhold: DeltakerRegistreringInnhold? = lagDeltakerRegistreringInnhold(),
    ): Tiltakstype {
        val tiltak = tiltakstypeCache[tiltakskode] ?: Tiltakstype(id, navn, tiltakskode, arenaKode, innsatsgrupper, innhold)
        val nyttTiltak = tiltak.copy(navn = navn, innhold = innhold)
        tiltakstypeCache[tiltak.tiltakskode] = nyttTiltak

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
                deltakerliste.tiltak.arenaKode.name,
            ),
            navn = deltakerliste.navn,
            startDato = deltakerliste.startDato,
            sluttDato = deltakerliste.sluttDato,
            status = deltakerliste.status.name,
            virksomhetsnummer = arrangor.organisasjonsnummer,
            oppstart = deltakerliste.oppstart,
            apentForPamelding = deltakerliste.apentForPamelding,
            antallPlasser = deltakerliste.antallPlasser,
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
        innhold: List<Innhold> = deltakerliste.tiltak.innhold
            ?.innholdselementer
            ?.map { it.toInnhold() } ?: emptyList(),
        status: DeltakerStatus = lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        historikk: Boolean = true,
        kanEndres: Boolean = true,
        sistEndret: LocalDateTime = LocalDateTime.now(),
        innsoktDatoFraArena: LocalDate? = null,
        erManueltDeltMedArrangor: Boolean = false,
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
            Deltakelsesinnhold("ledetekst", innhold),
            status,
            emptyList(),
            kanEndres,
            sistEndret,
            erManueltDeltMedArrangor,
        )

        return if (innsoktDatoFraArena != null) {
            deltaker.copy(historikk = lagArenaDeltakerHistorikk(deltaker, innsoktDatoFraArena))
        } else if (historikk) {
            deltaker.copy(
                historikk = lagDeltakerHistorikk(
                    deltaker = deltaker,
                ),
            )
        } else {
            deltaker
        }
    }

    fun lagTiltakskoordinatorDeltaker(
        sisteVurdering: Vurdering? = lagVurdering(),
        navEnhet: NavEnhet = lagNavEnhet(),
        navVeileder: NavAnsatt = lagNavAnsatt(),
        deltakerliste: Deltakerliste = lagDeltakerliste(),
    ): TiltakskoordinatorsDeltaker = lagDeltaker(
        deltakerliste = deltakerliste,
    ).toTiltakskoordinatorsDeltaker(
        sisteVurdering = sisteVurdering,
        navEnhet = navEnhet,
        navVeileder = navVeileder,
        null,
        false,
        emptyList(),
    )

    fun lagVurdering(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        opprettetAvArrangorAnsattId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now().minusMonths(1),
        vurderingstype: Vurderingstype = Vurderingstype.OPPFYLLER_IKKE_KRAVENE,
        begrunnelse: String? = "Begrunnelse på vurdering",
    ) = Vurdering(
        id = id,
        deltakerId = deltakerId,
        opprettetAvArrangorAnsattId = opprettetAvArrangorAnsattId,
        opprettet = opprettet,
        vurderingstype = vurderingstype,
        begrunnelse = begrunnelse,
    )

    private fun lagArenaDeltakerHistorikk(deltaker: Deltaker, innsoktDatoFraArena: LocalDate): List<DeltakerHistorikk> {
        val importertFraArena = lagImportertFraArena(deltaker = deltaker, innsoktDato = innsoktDatoFraArena)
        return listOf(DeltakerHistorikk.ImportertFraArena(importertFraArena))
    }

    private fun lagImportertFraArena(deltaker: Deltaker, innsoktDato: LocalDate) = ImportertFraArena(
        deltakerId = deltaker.id,
        importertDato = LocalDateTime.now(),
        deltakerVedImport = DeltakerVedImport(
            deltakerId = deltaker.id,
            innsoktDato = innsoktDato,
            startdato = deltaker.startdato,
            sluttdato = deltaker.sluttdato,
            dagerPerUke = deltaker.dagerPerUke,
            deltakelsesprosent = deltaker.deltakelsesprosent,
            status = deltaker.status,
        ),
    )

    private fun lagDeltakerHistorikk(deltaker: Deltaker): List<DeltakerHistorikk> {
        val vedtak = lagVedtak(
            deltakerVedVedtak = deltaker,
            fattet = LocalDateTime.now(),
            opprettetAv = deltaker.navBruker.navVeilederId!!,
            opprettetAvEnhet = deltaker.navBruker.navEnhetId!!,
        )
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
        forslag: Forslag? = null,
    ) = DeltakerEndring(id, deltakerId, endring, endretAv, endretAvEnhet, endret, forslag)

    fun lagForslag(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        opprettetAvArrangorAnsattId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        begrunnelse: String = "Begrunnelse fra arrangør",
        endring: Forslag.Endring = Forslag.ForlengDeltakelse(LocalDate.now().plusWeeks(2)),
        status: Forslag.Status = Forslag.Status.VenterPaSvar,
    ) = Forslag(id, deltakerId, opprettetAvArrangorAnsattId, opprettet, begrunnelse, endring, status)

    fun lagEndringFraArrangor(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        opprettetAvArrangorAnsattId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        endring: EndringFraArrangor.Endring = EndringFraArrangor.LeggTilOppstartsdato(
            LocalDate.now().plusDays(2),
            LocalDate.now().plusMonths(3),
        ),
    ) = EndringFraArrangor(id, deltakerId, opprettetAvArrangorAnsattId, opprettet, endring)

    fun lagNavAnsatt(
        id: UUID = UUID.randomUUID(),
        navIdent: String = randomNavIdent(),
        navn: String = "Veileder Veiledersen",
        epost: String = "veileder.veiledersen@nav.no",
        telefon: String = "12345678",
    ) = NavAnsatt(
        id = id,
        navIdent = navIdent,
        navn = navn,
        epost = epost,
        telefon = telefon,
        navEnhetId = null,
    )

    private val navEnhetCache = mutableMapOf<String, NavEnhet>()

    fun lagNavEnhet(
        id: UUID = UUID.randomUUID(),
        enhetsnummer: String = randomEnhetsnummer(),
        navn: String = "NAV Testheim",
    ): NavEnhet {
        val enhet = navEnhetCache[enhetsnummer] ?: NavEnhet(id, enhetsnummer, navn)
        val nyEnhet = enhet.copy(id = id, navn = navn)
        navEnhetCache[enhetsnummer] = nyEnhet
        return nyEnhet
    }

    fun lagNavBruker(
        personId: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        fornavn: String = "Fornavn",
        mellomnavn: String? = "Mellomnavn",
        etternavn: String = "Etternavn",
        adressebeskyttelse: Adressebeskyttelse? = null,
        oppfolgingsperioder: List<Oppfolgingsperiode> = listOf(lagOppfolgingsperiode()),
        innsatsgruppe: Innsatsgruppe? = Innsatsgruppe.STANDARD_INNSATS,
        adresse: Adresse? = lagAdresse(),
        erSkjermet: Boolean = false,
        navVeilederId: UUID = UUID.randomUUID(),
        navEnhetId: UUID = UUID.randomUUID(),
    ) = NavBruker(
        personId = personId,
        personident = personident,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        navVeilederId = navVeilederId,
        navEnhetId = navEnhetId,
        erSkjermet = erSkjermet,
        adresse = adresse,
        adressebeskyttelse = adressebeskyttelse,
        oppfolgingsperioder = oppfolgingsperioder,
        innsatsgruppe = innsatsgruppe,
        telefon = null,
        epost = null,
    )

    fun lagAdresse(): Adresse = Adresse(
        bostedsadresse = Bostedsadresse(
            coAdressenavn = "C/O Gutterommet",
            vegadresse = null,
            matrikkeladresse = Matrikkeladresse(
                tilleggsnavn = "Gården",
                postnummer = "0484",
                poststed = "OSLO",
            ),
        ),
        oppholdsadresse = null,
        kontaktadresse = Kontaktadresse(
            coAdressenavn = null,
            vegadresse = Vegadresse(
                husnummer = "1",
                husbokstav = null,
                adressenavn = "Gate",
                tilleggsnavn = null,
                postnummer = "1234",
                poststed = "MOSS",
            ),
            postboksadresse = null,
        ),
    )

    fun lagOppfolgingsperiode(
        id: UUID = UUID.randomUUID(),
        startdato: LocalDateTime = LocalDateTime.now().minusMonths(1),
        sluttdato: LocalDateTime? = null,
    ) = Oppfolgingsperiode(
        id,
        startdato,
        sluttdato,
    )

    private fun finnOppstartstype(type: Tiltakstype.ArenaKode) = when (type) {
        Tiltakstype.ArenaKode.JOBBK,
        Tiltakstype.ArenaKode.GRUPPEAMO,
        Tiltakstype.ArenaKode.GRUFAGYRKE,
        -> Deltakerliste.Oppstartstype.FELLES

        else -> Deltakerliste.Oppstartstype.LOPENDE
    }

    fun lagNavAnsatteForDeltaker(deltaker: Deltaker) = listOfNotNull(
        deltaker.vedtaksinformasjon?.sistEndretAv,
        deltaker.vedtaksinformasjon?.opprettetAv,
    ).distinct().map { lagNavAnsatt(id = it) }

    fun lagNavAnsatteForHistorikk(historikk: List<DeltakerHistorikk>) = historikk
        .flatMap { it.navAnsatte() }
        .distinct()
        .map { lagNavAnsatt(id = it) }

    fun lagNavEnheterForHistorikk(historikk: List<DeltakerHistorikk>) = historikk
        .flatMap { it.navEnheter() }
        .distinct()
        .map { lagNavEnhet(id = it) }

    fun leggTilHistorikk(
        deltaker: Deltaker,
        antallVedtak: Int = 1,
        antallEndringer: Int = 1,
        antallEndringerFraArrangor: Int = 1,
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

        val endringerFraArrangor = (1..antallEndringerFraArrangor).map { lagEndringFraArrangor(deltakerId = deltaker.id) }

        return deltaker.copy(
            historikk = vedtak.map { DeltakerHistorikk.Vedtak(it) } + endringer.map { DeltakerHistorikk.Endring(it) } +
                endringerFraArrangor.map { DeltakerHistorikk.EndringFraArrangor(it) },
        )
    }

    fun leggTilHistorikk(
        deltaker: Deltaker,
        vedtak: List<Vedtak> = emptyList(),
        endringer: List<DeltakerEndring> = emptyList(),
        forslag: List<Forslag> = emptyList(),
    ) = deltaker.copy(
        historikk = deltaker.historikk
            .plus(vedtak.map { DeltakerHistorikk.Vedtak(it) })
            .plus(endringer.map { DeltakerHistorikk.Endring(it) })
            .plus(forslag.map { DeltakerHistorikk.Forslag(it) }),
    )

    fun lagTiltakskoordinatorTilgang(
        id: UUID = UUID.randomUUID(),
        deltakerliste: Deltakerliste = lagDeltakerliste(),
        navAnsatt: NavAnsatt = lagNavAnsatt(),
        gyldigFra: LocalDateTime = LocalDateTime.now(),
        gyldigTil: LocalDateTime? = null,
    ) = TiltakskoordinatorDeltakerlisteTilgang(
        id = id,
        navAnsattId = navAnsatt.id,
        deltakerlisteId = deltakerliste.id,
        gyldigFra = gyldigFra,
        gyldigTil = gyldigTil,
    )

    fun lagEndringFraTiltakskoordinator(
        id: UUID = UUID.randomUUID(),
        deltakerId: UUID = UUID.randomUUID(),
        endring: EndringFraTiltakskoordinator.Endring = EndringFraTiltakskoordinator.DelMedArrangor,
        endretAv: UUID = UUID.randomUUID(),
        endretAvEnhet: UUID = UUID.randomUUID(),
        endret: LocalDateTime = LocalDateTime.now(),
    ) = EndringFraTiltakskoordinator(
        id = id,
        deltakerId = deltakerId,
        endring = endring,
        endretAv = endretAv,
        endretAvEnhet = endretAvEnhet,
        endret = endret,
    )

    fun lagHendelse(
        id: UUID = UUID.randomUUID(),
        deltaker: Deltaker,
        opprettet: LocalDateTime = LocalDateTime.now(),
        ansvarlig: HendelseAnsvarlig = HendelseAnsvarlig.NavVeileder(
            id = UUID.randomUUID(),
            navn = UUID.randomUUID().toString(),
            navIdent = UUID.randomUUID().toString(),
            enhet = HendelseAnsvarlig.NavVeileder.Enhet(id = UUID.randomUUID(), enhetsnummer = randomEnhetsnummer()),
        ),
        payload: HendelseType = HendelseType.EndreBakgrunnsinformasjon(
            bakgrunnsinformasjon = "Ny bakgrunnsinformasjon",
        ),
    ) = Hendelse(
        id = id,
        opprettet = opprettet,
        deltaker = HendelseDeltaker(
            id = deltaker.id,
            personident = randomIdent(),
            deltakerliste = HendelseDeltaker.Deltakerliste(
                id = deltaker.deltakerliste.id,
                navn = deltaker.deltakerliste.navn,
                arrangor = HendelseDeltaker.Deltakerliste.Arrangor(
                    id = deltaker.deltakerliste.arrangor.arrangor.id,
                    organisasjonsnummer = deltaker.deltakerliste.arrangor.arrangor.organisasjonsnummer,
                    navn = deltaker.deltakerliste.arrangor.arrangor.navn,
                    overordnetArrangor = null,
                ),
                tiltak = HendelseDeltaker.Deltakerliste.Tiltak(
                    navn = deltaker.deltakerliste.navn,
                    type = deltaker.deltakerliste.tiltak.arenaKode,
                    ledetekst = "ledetekst",
                    tiltakskode = deltaker.deltakerliste.tiltak.tiltakskode,
                ),
                startdato = deltaker.deltakerliste.startDato,
                sluttdato = deltaker.deltakerliste.sluttDato,
                oppstartstype = if (deltaker.deltakerliste.oppstart ==
                    Deltakerliste.Oppstartstype.FELLES
                ) {
                    HendelseDeltaker.Deltakerliste.Oppstartstype.FELLES
                } else {
                    HendelseDeltaker.Deltakerliste.Oppstartstype.LOPENDE
                },
            ),
            forsteVedtakFattet = LocalDate.now(),
            opprettetDato = LocalDate.now(),
        ),
        ansvarlig = ansvarlig,
        payload = payload,
    )
}

fun Deltaker.endre(deltakerEndring: DeltakerEndring): Deltaker {
    val deltaker = when (val endring = deltakerEndring.endring) {
        is DeltakerEndring.Endring.AvsluttDeltakelse -> this.copy(
            sluttdato = endring.sluttdato,
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = endring.aarsak?.toStatusAarsak()?.type,
                beskrivelse = endring.aarsak?.beskrivelse,
            ),
        )

        is DeltakerEndring.Endring.EndreAvslutning -> this.copy(
            status = TestData.lagDeltakerStatus(
                type = if (endring.harFullfort) DeltakerStatus.Type.FULLFORT else DeltakerStatus.Type.AVBRUTT,
                aarsak = endring.aarsak?.toStatusAarsak()?.type,
                beskrivelse = endring.aarsak?.beskrivelse,
            ),
        )

        is DeltakerEndring.Endring.AvbrytDeltakelse -> this.copy(
            sluttdato = endring.sluttdato,
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.AVBRUTT,
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

        is DeltakerEndring.Endring.EndreInnhold -> this.copy(deltakelsesinnhold = Deltakelsesinnhold(endring.ledetekst, endring.innhold))
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

        is DeltakerEndring.Endring.ReaktiverDeltakelse -> this.copy(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = null,
            sluttdato = null,
        )

        is DeltakerEndring.Endring.FjernOppstartsdato -> this.copy(startdato = null, sluttdato = null)
    }
    return deltaker.copy(historikk = this.historikk.plus(DeltakerHistorikk.Endring(deltakerEndring)))
}

fun DeltakerEndring.Aarsak.toStatusAarsak() = DeltakerStatus.Aarsak(
    type = DeltakerStatus.Aarsak.Type.valueOf(this.type.name),
    beskrivelse = this.beskrivelse,
)
