package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.UlestHendelse
import no.nav.amt.deltaker.bff.utils.prefixColumn
import no.nav.amt.lib.models.hendelse.Hendelse
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

class UlestHendelseRepository {
    companion object {
        fun rowMapper(row: Row, alias: String? = "uh"): UlestHendelse {
            val col = prefixColumn(alias)

            return UlestHendelse(
                id = row.uuid(col("id")),
                opprettet = row.localDateTime(col("opprettet")),
                deltakerId = row.uuid(col("deltaker_id")),
                ansvarlig = objectMapper.readValue(row.string(col("ansvarlig"))),
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

    fun getForDeltakere(deltakerIder: List<UUID>) = Database.query {
        val query = queryOf(
            """
            SELECT 
                uh.id as "uh.id",
                uh.deltaker_id as "uh.deltaker_id",
                uh.opprettet as "uh.opprettet",
                uh.ansvarlig as "uh.ansvarlig",
                uh.hendelse as "uh.hendelse"
            FROM ulest_hendelse uh
            WHERE uh.deltaker_id = any(:deltaker_ider);
            """.trimIndent(),
            mapOf("deltaker_ider" to deltakerIder.toTypedArray()),
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

    fun upsert(hendelse: Hendelse) = Database.query {
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
                    "id" to hendelse.id,
                    "deltaker_id" to hendelse.deltaker.id,
                    "opprettet" to hendelse.opprettet,
                    "ansvarlig" to toPGObject(hendelse.ansvarlig),
                    "hendelse" to toPGObject(hendelse.payload),
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
