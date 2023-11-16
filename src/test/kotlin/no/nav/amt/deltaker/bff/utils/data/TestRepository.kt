package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.Deltaker
import no.nav.amt.deltaker.bff.deltaker.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import org.slf4j.LoggerFactory
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

    fun insert(deltakerliste: Deltakerliste, arrangor: Arrangor = TestData.lagArrangor(deltakerliste.arrangorId)) {
        try {
            insert(arrangor)
        } catch (e: Exception) {
            log.warn("Arrangor med id ${arrangor.id} er allerede opprettet")
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
                        "arrangor_id" to deltakerliste.arrangorId,
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
                deltakelsesprosent, bakgrunnsinformasjon, mal, sist_endret_av, modified_at, created_at
            )
            values (
                :id, :personident, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :mal, :sistEndretAv, :modifiedAt, :createdAt
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
}
