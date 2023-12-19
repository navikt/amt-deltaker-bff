package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
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
        deltakerliste: Deltakerliste = TestData.lagDeltakerliste(id = deltaker.deltakerlisteId),
    ) = Database.query { session ->
        try {
            insert(deltakerliste)
        } catch (e: Exception) {
            log.warn("Deltakerliste med id ${deltakerliste.id} er allerede opprettet")
        }

        val sql = """
            insert into deltaker(
                id, personident, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, mal, sist_endret_av, sist_endret_av_enhet, modified_at, created_at
            )
            values (
                :id, :personident, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :mal, :sistEndretAv, :sistEndretAvEnhet, :modifiedAt, :createdAt
            )
        """.trimIndent()

        val parameters = mapOf(
            "id" to deltaker.id,
            "personident" to deltaker.personident,
            "deltakerlisteId" to deltaker.deltakerlisteId,
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
            insert into deltaker_samtykke (id, deltaker_id, godkjent, gyldig_til, deltaker_ved_samtykke, godkjent_av_nav)
            values (:id, :deltaker_id, :godkjent, :gyldig_til, :deltaker_ved_samtykke, :godkjent_av_nav)
            on conflict (id) do nothing;
        """.trimIndent()

        val params = mapOf(
            "id" to samtykke.id,
            "deltaker_id" to samtykke.deltakerId,
            "godkjent" to samtykke.godkjent,
            "gyldig_til" to samtykke.gyldigTil,
            "deltaker_ved_samtykke" to toPGObject(samtykke.deltakerVedSamtykke),
            "godkjent_av_nav" to samtykke.godkjentAvNav?.let(::toPGObject),
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
}
