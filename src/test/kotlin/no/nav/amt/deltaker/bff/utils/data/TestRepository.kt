package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetDbo
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Vedtak
import no.nav.amt.lib.utils.database.Database
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

object TestRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanDatabase() = Database.query { session ->
        val tables = listOf(
            "tiltakskoordinator_deltakerliste_tilgang",
            "forslag",
            "deltaker_status",
            "nav_ansatt",
            "nav_enhet",
            "deltaker",
            "nav_bruker",
            "deltakerliste",
            "arrangor",
        )
        tables.forEach {
            val query = queryOf(
                """delete from $it""",
                emptyMap(),
            )

            session.update(query)
        }
    }

    fun insert(arrangor: Arrangor) {
        val sql =
            """
            INSERT INTO arrangor(id, navn, organisasjonsnummer, overordnet_arrangor_id)
            VALUES (:id, :navn, :organisasjonsnummer, :overordnet_arrangor_id) 
            """.trimIndent()

        Database.query {
            val query = queryOf(
                sql,
                mapOf(
                    "id" to arrangor.id,
                    "navn" to arrangor.navn,
                    "organisasjonsnummer" to arrangor.organisasjonsnummer,
                    "overordnet_arrangor_id" to arrangor.overordnetArrangorId,
                ),
            )
            it.update(query)
        }
    }

    fun insert(tiltakstype: Tiltakstype) = Database.query {
        try {
            val sql =
                """
                INSERT INTO tiltakstype(
                    id, 
                    navn, 
                    tiltakskode,
                    type, 
                    innsatsgrupper,
                    innhold)
                VALUES (:id,
                        :navn,
                        :tiltakskode,
                        :type,
                        :innsatsgrupper,
                        :innhold)
                ON CONFLICT (id) DO UPDATE SET
                    navn = :navn,
                    innsatsgrupper = :innsatsgrupper,
                    innhold = :innhold
                """.trimIndent()

            it.update(
                queryOf(
                    sql,
                    mapOf(
                        "id" to tiltakstype.id,
                        "navn" to tiltakstype.navn,
                        "tiltakskode" to tiltakstype.tiltakskode.name,
                        "type" to tiltakstype.arenaKode.name,
                        "innsatsgrupper" to toPGObject(tiltakstype.innsatsgrupper),
                        "innhold" to toPGObject(tiltakstype.innhold),
                    ),
                ),
            )
        } catch (e: Exception) {
            log.warn("Tiltakstype ${tiltakstype.navn} er allerede opprettet", e)
        }
    }

    fun insert(deltakerliste: Deltakerliste, overordnetArrangor: Arrangor? = null) {
        try {
            insert(deltakerliste.tiltak)
        } catch (e: Exception) {
            log.warn("Tiltakstype  ${deltakerliste.tiltak.arenaKode} er allerede opprettet")
        }
        if (overordnetArrangor != null) {
            try {
                insert(overordnetArrangor)
            } catch (e: Exception) {
                log.warn("Overordnet arrangor med id ${overordnetArrangor.id} er allerede opprettet")
            }
        }
        try {
            insert(deltakerliste.arrangor.arrangor)
        } catch (e: Exception) {
            log.warn("Arrangor med id ${deltakerliste.arrangor.arrangor.id} er allerede opprettet")
        }

        Database.query {
            val sql =
                """
                INSERT INTO deltakerliste(id, navn, status, arrangor_id, tiltakstype_id, start_dato, slutt_dato, oppstart, antall_plasser, apent_for_pamelding)
                VALUES (:id, :navn, :status, :arrangor_id, :tiltakstype_id, :start_dato, :slutt_dato, :oppstart, :antall_plasser, :apent_for_pamelding)
                ON CONFLICT DO NOTHING
                """.trimIndent()

            it.update(
                queryOf(
                    sql,
                    mapOf(
                        "id" to deltakerliste.id,
                        "navn" to deltakerliste.navn,
                        "status" to deltakerliste.status.name,
                        "arrangor_id" to deltakerliste.arrangor.arrangor.id,
                        "tiltakstype_id" to deltakerliste.tiltak.id,
                        "start_dato" to deltakerliste.startDato,
                        "slutt_dato" to deltakerliste.sluttDato,
                        "oppstart" to deltakerliste.oppstart?.name,
                        "antall_plasser" to deltakerliste.antallPlasser,
                        "apent_for_pamelding" to deltakerliste.apentForPamelding,
                    ),
                ),
            )
        }
    }

    fun insert(deltaker: Deltaker) = Database.query { session ->
        try {
            insert(deltaker.navBruker)
        } catch (e: Exception) {
            log.warn("NavBruker med id ${deltaker.navBruker.personId} er allerede opprettet")
        }
        try {
            insert(deltaker.deltakerliste)
        } catch (e: Exception) {
            log.warn("Deltakerliste med id ${deltaker.deltakerliste.id} er allerede opprettet", e)
        }

        val sql =
            """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold, historikk, kan_endres, modified_at
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold, :historikk, :kan_endres, :modified_at
            )
            """.trimIndent()

        val parameters = mapOf(
            "id" to deltaker.id,
            "person_id" to deltaker.navBruker.personId,
            "deltakerlisteId" to deltaker.deltakerliste.id,
            "startdato" to deltaker.startdato,
            "sluttdato" to deltaker.sluttdato,
            "dagerPerUke" to deltaker.dagerPerUke,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
            "innhold" to toPGObject(deltaker.deltakelsesinnhold),
            "historikk" to toPGObject(deltaker.historikk),
            "kan_endres" to deltaker.kanEndres,
            "modified_at" to deltaker.sistEndret,
        )

        session.update(queryOf(sql, parameters))
        insert(deltaker.status, deltaker.id)
    }

    fun insert(status: DeltakerStatus, deltakerId: UUID) = Database.query {
        val sql =
            """
            insert into deltaker_status(id, deltaker_id, type, aarsak, gyldig_fra, gyldig_til, created_at) 
            values (:id, :deltaker_id, :type, :aarsak, :gyldig_fra, :gyldig_til, :created_at) 
            on conflict (id) do nothing;
            """.trimIndent()

        val params = mapOf(
            "id" to status.id,
            "deltaker_id" to deltakerId,
            "type" to status.type.name,
            "aarsak" to toPGObject(status.aarsak),
            "gyldig_fra" to status.gyldigFra,
            "gyldig_til" to status.gyldigTil,
            "created_at" to status.opprettet,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(vedtak: Vedtak) = Database.query {
        val sql =
            """
            insert into vedtak (id,
                                deltaker_id,
                                fattet,
                                gyldig_til,
                                deltaker_ved_vedtak,
                                fattet_av_nav,
                                created_at,
                                opprettet_av,
                                opprettet_av_enhet,
                                modified_at,
                                sist_endret_av,
                                sist_endret_av_enhet)
            values (:id,
                    :deltaker_id,
                    :fattet, :gyldig_til,
                    :deltaker_ved_vedtak,
                    :fattet_av_nav,
                    :opprettet,
                    :opprettet_av,
                    :opprettet_av_enhet,
                    :sist_endret,
                    :sist_endret_av,
                    :sist_endret_av_enhet)
            """.trimIndent()

        val params = mapOf(
            "id" to vedtak.id,
            "deltaker_id" to vedtak.deltakerId,
            "fattet" to vedtak.fattet,
            "gyldig_til" to vedtak.gyldigTil,
            "deltaker_ved_vedtak" to toPGObject(vedtak.deltakerVedVedtak),
            "fattet_av_nav" to vedtak.fattetAvNav,
            "opprettet" to vedtak.opprettet,
            "opprettet_av" to vedtak.opprettetAv,
            "opprettet_av_enhet" to vedtak.opprettetAvEnhet,
            "sist_endret" to vedtak.sistEndret,
            "sist_endret_av" to vedtak.sistEndretAv,
            "sist_endret_av_enhet" to vedtak.sistEndretAvEnhet,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(navAnsatt: NavAnsatt) = Database.query {
        val sql =
            """
            insert into nav_ansatt(id, nav_ident, navn, modified_at)
            values (:id, :nav_ident, :navn, :modified_at) 
            on conflict (id) do nothing;
            """.trimIndent()

        val params = mapOf(
            "id" to navAnsatt.id,
            "nav_ident" to navAnsatt.navIdent,
            "navn" to navAnsatt.navn,
            "modified_at" to LocalDateTime.now(),
        )

        it.update(queryOf(sql, params))
    }

    fun insert(navEnhetDbo: NavEnhetDbo) = Database.query {
        val sql =
            """
            insert into nav_enhet(id, nav_enhet_nummer, navn, modified_at)
            values (:id, :nav_enhet_nummer, :navn, :modified_at) 
            on conflict (id) do nothing;
            """.trimIndent()

        val params = mapOf(
            "id" to navEnhetDbo.id,
            "nav_enhet_nummer" to navEnhetDbo.enhetsnummer,
            "navn" to navEnhetDbo.navn,
            "modified_at" to navEnhetDbo.sistEndret,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(navEnhet: NavEnhet) = Database.query {
        val sql =
            """
            insert into nav_enhet(id, nav_enhet_nummer, navn)
            values (:id, :nav_enhet_nummer, :navn) 
            on conflict (id) do nothing;
            """.trimIndent()

        val params = mapOf(
            "id" to navEnhet.id,
            "nav_enhet_nummer" to navEnhet.enhetsnummer,
            "navn" to navEnhet.navn,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(bruker: NavBruker) = Database.query {
        val sql =
            """
            insert into nav_bruker(person_id, personident, fornavn, mellomnavn, etternavn, adressebeskyttelse, oppfolgingsperioder, innsatsgruppe, adresse, er_skjermet) 
            values (:person_id, :personident, :fornavn, :mellomnavn, :etternavn, :adressebeskyttelse, :oppfolgingsperioder, :innsatsgruppe, :adresse, :er_skjermet)
            """.trimIndent()

        val params = mapOf(
            "person_id" to bruker.personId,
            "personident" to bruker.personident,
            "fornavn" to bruker.fornavn,
            "mellomnavn" to bruker.mellomnavn,
            "etternavn" to bruker.etternavn,
            "adressebeskyttelse" to bruker.adressebeskyttelse?.name,
            "oppfolgingsperioder" to toPGObject(bruker.oppfolgingsperioder),
            "innsatsgruppe" to bruker.innsatsgruppe?.name,
            "adresse" to toPGObject(bruker.adresse),
            "er_skjermet" to bruker.erSkjermet,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(tilgang: TiltakskoordinatorDeltakerlisteTilgang) = Database.query {
        val sql =
            """
            insert into tiltakskoordinator_deltakerliste_tilgang 
                (id, nav_ansatt_id, deltakerliste_id, gyldig_fra, gyldig_til)
            values (:id, :nav_ansatt_id, :deltakerliste_id, :gyldig_fra, :gyldig_til)
            on conflict (id) do update set id               = :id,
                                      nav_ansatt_id    = :nav_ansatt_id,
                                      deltakerliste_id = :deltakerliste_id,
                                      gyldig_fra       = :gyldig_fra,
                                      gyldig_til       = :gyldig_til,
                                      modified_at      = current_timestamp
            """.trimIndent()
        val params = mapOf(
            "id" to tilgang.id,
            "nav_ansatt_id" to tilgang.navAnsattId,
            "deltakerliste_id" to tilgang.deltakerlisteId,
            "gyldig_fra" to tilgang.gyldigFra,
            "gyldig_til" to tilgang.gyldigTil,
        )
        it.update(queryOf(sql, params))
    }

    fun getDeltakerSistBesokt(deltakerId: UUID) = Database.query {
        val sql =
            """
            select sist_besokt from deltaker where id = ?
            """.trimIndent()

        it.run(queryOf(sql, deltakerId).map { row -> row.zonedDateTime("sist_besokt") }.asSingle)
    }
}
