package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.db.DbUtils.nullWhenNearNow
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerStatusRepository.deaktiverUkritiskTidligereStatuserQuery
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

object DeltakerStatusRepository {
    /**
     * Henter ID-ene til deltakere som har mer enn én aktiv (gyldig) status registrert samtidig.
     *
     * En deltaker kun skal ha én aktiv status om gangen, i motsatt fall må koden rette opp i dette ved kall
     * til [deaktiverUkritiskTidligereStatuserQuery].
     * En status regnes som aktiv hvis feltet `gyldig_til` er `NULL`.
     *
     * @return En liste med deltaker-ID-er (UUID) som har flere aktive statuser.
     */
    fun getDeltakereMedFlereGyldigeStatuser(): List<UUID> {
        val sql =
            """
            SELECT deltaker_id
            FROM deltaker_status
            WHERE gyldig_til IS NULL
            GROUP BY deltaker_id
            HAVING COUNT(*) > 1
            """.trimIndent()

        val query = queryOf(sql).map { it.uuid("deltaker_id") }.asList

        return Database.query { session -> session.run(query) }
    }

    fun batchInsert(deltakere: List<Deltakeroppdatering>) {
        val statusParams = deltakere.map { insertStatusParams(it.status, it.id) }

        Database.query { session ->
            session.batchPreparedNamedStatement(insertStatusSQL, statusParams)
        }
    }

    fun batchDeaktiverTidligereStatuser(deltakere: List<Deltakeroppdatering>) {
        val deaktiverTidligereStatuserParams = deltakere.map { deaktiverTidligereStatuserParams(it.status, it.id) }

        Database.query { session ->
            session.batchPreparedNamedStatement(deaktiverTidligereStatuserSQL, deaktiverTidligereStatuserParams)
        }
    }

    /**
     * Setter inn en ny rad i tabellen `deltaker_status`.
     *
     * - Feltet `gyldig_fra` settes enten til verdien fra [DeltakerStatus.gyldigFra], eller til
     *   `CURRENT_TIMESTAMP` i databasen dersom tidspunktet er "nær nok" nåværende tidspunkt
     *   (se [nullWhenNearNow]).
     *
     * - Feltet `created_at` settes tilsvarende, basert på [DeltakerStatus.opprettet].
     *
     * - Konflikter på primærnøkkelen `id` ignoreres (`ON CONFLICT (id) DO NOTHING`).
     *
     * @param deltakerStatus status-objektet som skal lagres i databasen.
     * @param deltakerId ID-en til deltakeren statusen tilhører.
     * @return en ferdig parametrisert [Query] som kan kjøres mot databasen.
     */
    fun lagreStatus(deltakerId: UUID, deltakerStatus: DeltakerStatus) {
        val sql =
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
                COALESCE(:gyldig_fra, CURRENT_TIMESTAMP), 
                COALESCE(:created_at, CURRENT_TIMESTAMP))
            ON CONFLICT (id) DO NOTHING
            """.trimIndent()

        val params = mapOf(
            "id" to deltakerStatus.id,
            "deltaker_id" to deltakerId,
            "type" to deltakerStatus.type.name,
            "aarsak" to toPGObject(deltakerStatus.aarsak),
            "gyldig_fra" to nullWhenNearNow(deltakerStatus.gyldigFra),
            "created_at" to nullWhenNearNow(deltakerStatus.opprettet),
        )

        Database.query { session ->
            session.update(queryOf(sql, params))
        }
    }

    fun deaktiverTidligereStatuser(deltakerId: UUID, deltakerStatus: DeltakerStatus) {
        val sql =
            """
            UPDATE deltaker_status
            SET 
                gyldig_til = CURRENT_TIMESTAMP,
                modified_at = CURRENT_TIMESTAMP
            WHERE 
                deltaker_id = :deltaker_id 
                AND id != :id 
                AND gyldig_til IS NULL
            """.trimIndent()

        return Database.query { session ->
            session.update(
                queryOf(
                    sql,
                    mapOf("id" to deltakerStatus.id, "deltaker_id" to deltakerId),
                ),
            )
        }
    }

    fun getDeltakerStatuser(deltakerId: UUID): List<DeltakerStatus> {
        val sql = "SELECT * FROM deltaker_status WHERE deltaker_id = :deltaker_id"

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId))
            .map(::deltakerStatusRowMapper)
            .asList

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

    fun deaktiverUkritiskTidligereStatuserQuery(status: DeltakerStatus, deltakerId: UUID) {
        val sql =
            """
            UPDATE deltaker_status
            SET gyldig_til = CURRENT_TIMESTAMP
            WHERE 
                deltaker_id = :deltaker_id 
                AND id != :id 
                AND gyldig_til IS NULL
            """.trimIndent()

        val query = queryOf(
            sql,
            mapOf("id" to status.id, "deltaker_id" to deltakerId),
        )

        Database.query { session -> session.update(query) }
    }

    private fun deltakerStatusRowMapper(row: Row) = DeltakerStatus(
        id = row.uuid("id"),
        type = row.string("type").let { t -> DeltakerStatus.Type.valueOf(t) },
        aarsak = row.stringOrNull("aarsak")?.let { aarsak -> objectMapper.readValue(aarsak) },
        gyldigFra = row.localDateTime("gyldig_fra"),
        gyldigTil = row.localDateTimeOrNull("gyldig_til"),
        opprettet = row.localDateTime("created_at"),
    )

    private val insertStatusSQL =
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

    private fun insertStatusParams(status: DeltakerStatus, deltakerId: UUID) = mapOf(
        "id" to status.id,
        "deltaker_id" to deltakerId,
        "type" to status.type.name,
        "aarsak" to toPGObject(status.aarsak),
        "gyldig_fra" to status.gyldigFra,
        "created_at" to status.opprettet,
    )

    private val deaktiverTidligereStatuserSQL =
        """
        UPDATE deltaker_status
        SET gyldig_til = CURRENT_TIMESTAMP
        WHERE 
            deltaker_id = :deltaker_id 
            AND id != :id 
            AND gyldig_til IS NULL
        """.trimIndent()

    private fun deaktiverTidligereStatuserParams(status: DeltakerStatus, deltakerId: UUID) =
        mapOf("id" to status.id, "deltaker_id" to deltakerId)
}
