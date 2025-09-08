package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.UlestHendelse
import no.nav.amt.deltaker.bff.utils.prefixColumn
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

class UlestHendelseRepository {
    companion object {
        fun rowMapper(row: Row, alias: String? = "uh"): UlestHendelse {
            val col = prefixColumn(alias)

            return UlestHendelse(
                id = row.uuid(col("id")),
                opprettet = row.localDateTime(col("opprettet")),
                deltakerId = row.uuid(col("deltaker_id")),
                ansvarlig = row.stringOrNull(col("ansvarlig"))?.let { objectMapper.readValue(it) },
                hendelse = objectMapper.readValue(row.string(col("hendelse"))),
            )
        }
    }

    fun getForDeltaker(deltakerId: UUID) = Database.query {
        val query = queryOf(
            """
            SELECT 
                uh.id as "uh.id",
                uh.deltaker_id as "uh.deltaker_id",
                uh.opprettet as "uh.opprettet",
                uh.ansvarlig as "uh.ansvarlig",
                uh.hendelse as "uh.hendelse"
            FROM ulest_hendelse uh
            WHERE uh.deltaker_id = :deltaker_id;
            """.trimIndent(),
            mapOf("deltaker_id" to deltakerId),
        )
        it.run(query.map(::rowMapper).asList)
    }

    fun get(id: UUID) = Database.query {
        val query = queryOf(
            """
            SELECT 
                 uh.id as "uh.id",
                 uh.deltaker_id as "uh.deltaker_id",
                 uh.opprettet as "uh.opprettet",
                 uh.ansvarlig as "uh.ansvarlig",
                 uh.hendelse as "uh.hendelse"
             FROM ulest_hendelse uh
             WHERE uh.id = :id;
            """.trimIndent(),
            mapOf("id" to id),
        ).map(::rowMapper).asSingle
        it.run(query)?.let { d -> Result.success(d) }
            ?: Result.failure(NoSuchElementException("Ingen ulest hendelse med id $id"))
    }

    fun upsert(ulestHendelse: UlestHendelse) = Database.query {
        val sql =
            """
            INSERT INTO ulest_hendelse(
                id, 
                deltaker_id, 
                opprettet, 
                ansvarlig, 
                hendelse)
            VALUES (:id,
            		:deltaker_id,
            		:opprettet,
            		:ansvarlig,
                    :hendelse)
            ON CONFLICT (id) DO UPDATE SET
            		deltaker_id     	= :deltaker_id,
            		opprettet 			= :opprettet,
            		ansvarlig			= :ansvarlig,
            		hendelse			= :hendelse,
                    modified_at         = current_timestamp
            """.trimIndent()

        it.update(
            queryOf(
                sql,
                mapOf(
                    "id" to ulestHendelse.id,
                    "deltaker_id" to ulestHendelse.deltakerId,
                    "opprettet" to ulestHendelse.opprettet,
                    "ansvarlig" to toPGObject(ulestHendelse.ansvarlig),
                    "hendelse" to toPGObject(ulestHendelse.hendelse),
                ),
            ),
        )
    }

    fun delete(id: UUID) = Database.query {
        val query = queryOf(
            """delete from ulest_hendelse where id = :id""",
            mapOf("id" to id),
        )
        it.update(query)
    }
}
