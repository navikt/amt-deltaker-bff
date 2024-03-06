package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.endringsmelding.Endringsmelding
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

object TestRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanDatabase() = Database.query { session ->
        val tables = listOf(
            "deltaker_status",
            "endringsmelding",
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
        val sql = """
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
            val sql = """
                INSERT INTO tiltakstype(
                    id, 
                    navn, 
                    type, 
                    innhold)
                VALUES (:id,
                        :navn,
                        :type,
                        :innhold)
            """.trimIndent()

            it.update(
                queryOf(
                    sql,
                    mapOf(
                        "id" to tiltakstype.id,
                        "navn" to tiltakstype.navn,
                        "type" to tiltakstype.type.name,
                        "innhold" to toPGObject(tiltakstype.innhold),
                    ),
                ),
            )
        } catch (e: Exception) {
            log.warn("Tiltakstype ${tiltakstype.navn} er allerede opprettet")
        }
    }

    fun insert(deltakerliste: Deltakerliste, overordnetArrangor: Arrangor? = null) {
        try {
            insert(deltakerliste.tiltak)
        } catch (e: Exception) {
            log.warn("Tiltakstype  ${deltakerliste.tiltak.type} er allerede opprettet")
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
            val sql = """
                INSERT INTO deltakerliste( id, navn, status, arrangor_id, tiltaksnavn, tiltakstype, start_dato, slutt_dato, oppstart)
                VALUES (:id, :navn, :status, :arrangor_id, :tiltaksnavn, :tiltakstype, :start_dato, :slutt_dato, :oppstart) 
            """.trimIndent()

            it.update(
                queryOf(
                    sql,
                    mapOf(
                        "id" to deltakerliste.id,
                        "navn" to deltakerliste.navn,
                        "status" to deltakerliste.status.name,
                        "arrangor_id" to deltakerliste.arrangor.arrangor.id,
                        "tiltaksnavn" to deltakerliste.tiltak.navn,
                        "tiltakstype" to deltakerliste.tiltak.type.name,
                        "start_dato" to deltakerliste.startDato,
                        "slutt_dato" to deltakerliste.sluttDato,
                        "oppstart" to deltakerliste.oppstart?.name,
                    ),
                ),
            )
        }
    }

    fun insert(
        deltaker: Deltaker,
    ) = Database.query { session ->
        try {
            insert(deltaker.navBruker)
        } catch (e: Exception) {
            log.warn("NavBruker med id ${deltaker.navBruker.personId} er allerede opprettet")
        }
        try {
            insert(deltaker.deltakerliste)
        } catch (e: Exception) {
            log.warn("Deltakerliste med id ${deltaker.deltakerliste.id} er allerede opprettet")
        }

        val sql = """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold, historikk
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold, :historikk
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
            "innhold" to toPGObject(deltaker.innhold),
            "historikk" to toPGObject(deltaker.historikk.map { objectMapper.writeValueAsString(it) }),
        )

        session.update(queryOf(sql, parameters))
        insert(deltaker.status, deltaker.id)
    }

    fun insert(status: DeltakerStatus, deltakerId: UUID) = Database.query {
        val sql = """
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
        val sql = """
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
        val sql = """
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

    fun insert(navEnhet: NavEnhet) = Database.query {
        val sql = """
            insert into nav_enhet(id, nav_enhet_nummer, navn, modified_at)
            values (:id, :nav_enhet_nummer, :navn, :modified_at) 
            on conflict (id) do nothing;
        """.trimIndent()

        val params = mapOf(
            "id" to navEnhet.id,
            "nav_enhet_nummer" to navEnhet.enhetsnummer,
            "navn" to navEnhet.navn,
            "modified_at" to LocalDateTime.now(),
        )

        it.update(queryOf(sql, params))
    }

    fun insert(endringsmelding: Endringsmelding) = Database.query {
        try {
            insert(TestData.lagDeltaker(endringsmelding.deltakerId))
        } catch (e: Exception) {
            log.warn("Deltaker med id ${endringsmelding.deltakerId} finnes fra før")
        }

        val sql = """
            insert into endringsmelding(
                id,
                deltaker_id, 
                utfort_av_nav_ansatt_id, 
                opprettet_av_arrangor_ansatt_id, 
                utfort_tidspunkt,
                status, 
                type, 
                innhold, 
                created_at
            )
            values (
                :id, 
                :deltaker_id, 
                :utfort_av_nav_ansatt_id, 
                :opprettet_av_arrangor_ansatt_id, 
                :utfort_tidspunkt,
                :status, 
                :type, 
                :innhold, 
                :created_at
            )
        """.trimIndent()

        val params = mapOf(
            "id" to endringsmelding.id,
            "deltaker_id" to endringsmelding.deltakerId,
            "utfort_av_nav_ansatt_id" to endringsmelding.utfortAvNavAnsattId,
            "opprettet_av_arrangor_ansatt_id" to endringsmelding.opprettetAvArrangorAnsattId,
            "utfort_tidspunkt" to endringsmelding.utfortTidspunkt,
            "status" to endringsmelding.status.name,
            "type" to endringsmelding.type.name,
            "innhold" to toPGObject(endringsmelding.innhold),
            "created_at" to endringsmelding.createdAt,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(bruker: NavBruker) = Database.query {
        val sql = """
            insert into nav_bruker(person_id, personident, fornavn, mellomnavn, etternavn) 
            values (:person_id, :personident, :fornavn, :mellomnavn, :etternavn)
        """.trimIndent()

        val params = mapOf(
            "person_id" to bruker.personId,
            "personident" to bruker.personident,
            "fornavn" to bruker.fornavn,
            "mellomnavn" to bruker.mellomnavn,
            "etternavn" to bruker.etternavn,
        )

        it.update(queryOf(sql, params))
    }
}
