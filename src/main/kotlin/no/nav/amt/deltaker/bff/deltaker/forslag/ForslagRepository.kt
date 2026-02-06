package no.nav.amt.deltaker.bff.deltaker.forslag

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.utils.prefixColumn
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

class ForslagRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getForDeltaker(deltakerId: UUID): List<Forslag> {
        val query = queryOf(
            """
            SELECT 
                f.id as "f.id",
                f.deltaker_id as "f.deltaker_id",
                f.arrangoransatt_id as "f.arrangoransatt_id",
                f.opprettet as "f.opprettet",
                f.begrunnelse as "f.begrunnelse",
                f.endring as "f.endring",
                f.status as "f.status"
            FROM forslag f 
            WHERE f.deltaker_id = :deltaker_id;
            """.trimIndent(),
            mapOf("deltaker_id" to deltakerId),
        )

        return Database.query { session ->
            session.run(query.map(::rowMapper).asList)
        }
    }

    fun getForDeltakere(deltakerIder: List<UUID>): List<Forslag> {
        val query = queryOf(
            """
            SELECT 
                f.id as "f.id",
                f.deltaker_id as "f.deltaker_id",
                f.arrangoransatt_id as "f.arrangoransatt_id",
                f.opprettet as "f.opprettet",
                f.begrunnelse as "f.begrunnelse",
                f.endring as "f.endring",
                f.status as "f.status"
            FROM forslag f 
            WHERE f.deltaker_id = ANY(:deltaker_ider::uuid[]);
            """.trimIndent(),
            mapOf("deltaker_ider" to deltakerIder.toTypedArray()),
        )

        return Database.query { session ->
            session.run(query.map(::rowMapper).asList)
        }
    }

    fun get(id: UUID): Result<Forslag> = runCatching {
        val query = queryOf(
            """
            SELECT 
                f.id as "f.id",
                f.deltaker_id as "f.deltaker_id",
                f.arrangoransatt_id as "f.arrangoransatt_id",
                f.opprettet as "f.opprettet",
                f.begrunnelse as "f.begrunnelse",
                f.endring as "f.endring",
                f.status as "f.status"
            FROM forslag f 
            WHERE f.id = :id;
            """.trimIndent(),
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        Database.query { session ->
            session.run(query) ?: throw NoSuchElementException("Ingen forslag med id $id")
        }
    }

    fun upsert(forslag: Forslag) {
        val sql =
            """
            INSERT INTO forslag (
                id, 
                deltaker_id, 
                arrangoransatt_id, 
                opprettet, 
                begrunnelse, 
                endring,  
                status
            )
            VALUES (
                :id,
                :deltaker_id,
                :arrangoransatt_id,
                :opprettet,
                :begrunnelse,
                :endring,
                :status
            )
            ON CONFLICT (id) DO UPDATE SET
                deltaker_id     	= :deltaker_id,
                arrangoransatt_id	= :arrangoransatt_id,
                opprettet 			= :opprettet,
                begrunnelse			= :begrunnelse,
                endring				= :endring,
                status              = :status,
                modified_at         = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to forslag.id,
            "deltaker_id" to forslag.deltakerId,
            "arrangoransatt_id" to forslag.opprettetAvArrangorAnsattId,
            "opprettet" to forslag.opprettet,
            "begrunnelse" to forslag.begrunnelse,
            "endring" to toPGObject(forslag.endring),
            "status" to toPGObject(forslag.status),
        )

        Database.query { session ->
            session.update(queryOf(sql, params))
        }
    }

    fun delete(id: UUID) {
        val query = queryOf(
            "DELETE FROM forslag WHERE id = :id",
            mapOf("id" to id),
        )

        Database.query { session -> session.update(query) }
        log.info("Slettet godkjent forslag $id")
    }

    fun deleteForDeltaker(deltakerId: UUID) {
        val query = queryOf(
            "DELETE FROM forslag WHERE deltaker_id = :deltaker_id",
            mapOf("deltaker_id" to deltakerId),
        )

        Database.query { session -> session.update(query) }
    }

    fun kanLagres(deltakerId: UUID): Boolean {
        val query = queryOf(
            "SELECT id FROM deltaker WHERE id = :id",
            mapOf("id" to deltakerId),
        ).map { row -> row.uuid("id") }.asSingle

        return Database.query { it.run(query) } != null
    }

    companion object {
        private fun rowMapper(row: Row, alias: String? = "f"): Forslag {
            val col = prefixColumn(alias)

            return Forslag(
                id = row.uuid(col("id")),
                deltakerId = row.uuid(col("deltaker_id")),
                opprettetAvArrangorAnsattId = row.uuid(col("arrangoransatt_id")),
                opprettet = row.localDateTime(col("opprettet")),
                begrunnelse = row.stringOrNull(col("begrunnelse")),
                endring = objectMapper.readValue(row.string(col("endring"))),
                status = objectMapper.readValue(row.string(col("status"))),
            )
        }
    }
}
