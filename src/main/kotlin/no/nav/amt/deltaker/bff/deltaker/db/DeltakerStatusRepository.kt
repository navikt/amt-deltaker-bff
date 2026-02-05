package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

object DeltakerStatusRepository {
    fun insertIfNotExists(deltakerId: UUID, deltakerStatus: DeltakerStatus) {
        Database.query { session ->
            session.update(
                queryOf(
                    insertStatusSql,
                    buildInsertStatusParams(deltakerStatus, deltakerId),
                ),
            )
        }
    }

    fun batchInsert(deltakere: List<Deltakeroppdatering>) {
        val statusParamsList = deltakere.map { buildInsertStatusParams(it.status, it.id) }

        Database.query { session ->
            session.batchPreparedNamedStatement(insertStatusSql, statusParamsList)
        }
    }

    fun deaktiverTidligereStatuser(deltakerId: UUID, deltakerStatus: DeltakerStatus) {
        Database.query { session ->
            session.update(
                queryOf(
                    deaktiverTidligereStatuserSql,
                    buildDeaktiverTidligereStatuserParams(deltakerStatus, deltakerId),
                ),
            )
        }
    }

    fun batchDeaktiverTidligereStatuser(deltakere: List<Deltakeroppdatering>) {
        val deaktiverTidligereStatuserParams = deltakere
            .map { buildDeaktiverTidligereStatuserParams(it.status, it.id) }

        Database.query { session ->
            session.batchPreparedNamedStatement(deaktiverTidligereStatuserSql, deaktiverTidligereStatuserParams)
        }
    }

    fun getAktivDeltakerStatus(deltakerId: UUID): DeltakerStatus? {
        val sql = "SELECT * FROM deltaker_status WHERE deltaker_id = :deltaker_id AND gyldig_til IS NULL"

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId))
            .map(::deltakerStatusRowMapper)
            .asSingle

        return Database.query { session -> session.run(query) }
    }

    fun slettStatus(deltakerId: UUID) {
        Database.query { session ->
            session.update(
                queryOf(
                    "DELETE FROM deltaker_status WHERE deltaker_id = :deltaker_id",
                    mapOf("deltaker_id" to deltakerId),
                ),
            )
        }
    }

    private fun deltakerStatusRowMapper(row: Row) = DeltakerStatus(
        id = row.uuid("id"),
        type = row.string("type").let { t -> DeltakerStatus.Type.valueOf(t) },
        aarsak = row.stringOrNull("aarsak")?.let { aarsak -> objectMapper.readValue(aarsak) },
        gyldigFra = row.localDateTime("gyldig_fra"),
        gyldigTil = row.localDateTimeOrNull("gyldig_til"),
        opprettet = row.localDateTime("created_at"),
    )

    private val insertStatusSql =
        """
        INSERT INTO deltaker_status (
            id, 
            deltaker_id, 
            type, 
            aarsak, 
            gyldig_fra, 
            created_at
        )
        VALUES (
            :id, 
            :deltaker_id, 
            :type, 
            :aarsak, 
            :gyldig_fra, 
            :created_at
        )
        ON CONFLICT (id) DO NOTHING
        """.trimIndent()

    private fun buildInsertStatusParams(status: DeltakerStatus, deltakerId: UUID) = mapOf(
        "id" to status.id,
        "deltaker_id" to deltakerId,
        "type" to status.type.name,
        "aarsak" to toPGObject(status.aarsak),
        "gyldig_fra" to status.gyldigFra,
        "created_at" to status.opprettet,
    )

    private val deaktiverTidligereStatuserSql =
        """
        UPDATE deltaker_status
        SET gyldig_til = CURRENT_TIMESTAMP
        WHERE 
            deltaker_id = :deltaker_id 
            AND id != :id 
            AND gyldig_til IS NULL
        """.trimIndent()

    private fun buildDeaktiverTidligereStatuserParams(status: DeltakerStatus, deltakerId: UUID) =
        mapOf("id" to status.id, "deltaker_id" to deltakerId)
}
