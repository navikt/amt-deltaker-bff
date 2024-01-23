package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.endringsmelding.Endringsmelding
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

object TestRepository {
    private val log = LoggerFactory.getLogger(javaClass)
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

    fun insert(deltakerliste: Deltakerliste) {
        try {
            insert(deltakerliste.arrangor)
        } catch (e: Exception) {
            log.warn("Arrangor med id ${deltakerliste.arrangor.id} er allerede opprettet")
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
                        "arrangor_id" to deltakerliste.arrangor.id,
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
                deltakelsesprosent, bakgrunnsinformasjon, mal, sist_endret_av, sist_endret_av_enhet, modified_at, created_at
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :mal, :sistEndretAv, :sistEndretAvEnhet, :modifiedAt, :createdAt
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
            "mal" to toPGObject(deltaker.mal),
            "sistEndretAv" to deltaker.sistEndretAv,
            "sistEndretAvEnhet" to deltaker.sistEndretAvEnhet,
            "modifiedAt" to deltaker.sistEndret,
            "createdAt" to deltaker.opprettet,
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
            "aarsak" to status.aarsak?.name,
            "gyldig_fra" to status.gyldigFra,
            "gyldig_til" to status.gyldigTil,
            "created_at" to status.opprettet,
        )

        it.update(queryOf(sql, params))
    }

    fun insert(samtykke: DeltakerSamtykke) = Database.query {
        val sql = """
            insert into deltaker_samtykke (id, deltaker_id, godkjent, gyldig_til, deltaker_ved_samtykke, godkjent_av_nav, created_at, opprettet_av, opprettet_av_enhet)
            values (:id, :deltaker_id, :godkjent, :gyldig_til, :deltaker_ved_samtykke, :godkjent_av_nav, :created_at, :opprettet_av, :opprettet_av_enhet)
            on conflict (id) do nothing;
        """.trimIndent()

        val params = mapOf(
            "id" to samtykke.id,
            "deltaker_id" to samtykke.deltakerId,
            "godkjent" to samtykke.godkjent,
            "gyldig_til" to samtykke.gyldigTil,
            "deltaker_ved_samtykke" to toPGObject(samtykke.deltakerVedSamtykke),
            "godkjent_av_nav" to samtykke.godkjentAvNav?.let(::toPGObject),
            "created_at" to samtykke.opprettet,
            "opprettet_av" to samtykke.opprettetAv,
            "opprettet_av_enhet" to samtykke.opprettetAvEnhet,
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

    fun insert(endring: DeltakerEndring) = Database.query {
        val sql = """
            insert into deltaker_endring (id, deltaker_id, endringstype, endring, endret_av, endret_av_enhet, modified_at)
            values (:id, :deltaker_id, :endringstype, :endring, :endret_av, :endret_av_enhet, :endret)
            on conflict (id) do update set 
                deltaker_id = :deltaker_id,
                endringstype = :endringstype,
                endring = :endring,
                endret_av = :endret_av,
                endret_av_enhet = :endret_av_enhet,
                modified_at = :endret
        """.trimIndent()

        val params = mapOf(
            "id" to endring.id,
            "deltaker_id" to endring.deltakerId,
            "endringstype" to endring.endringstype.name,
            "endring" to toPGObject(endring.endring),
            "endret_av" to endring.endretAv,
            "endret_av_enhet" to endring.endretAvEnhet,
            "endret" to endring.endret,
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
