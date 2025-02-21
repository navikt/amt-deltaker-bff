package no.nav.amt.deltaker.bff.deltaker.vurdering

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

class VurderingRepository {
    companion object {
        fun rowMapper(row: Row): Vurdering {
            return Vurdering(
                id = row.uuid("id"),
                deltakerId = row.uuid("deltaker_id"),
                opprettetAvArrangorAnsattId = row.uuid("opprettet_av_arrangor_ansatt_id"),
                opprettet = row.localDateTime("opprettet"),
                vurderingstype = Vurderingstype.valueOf(row.string("vurderingstype")),
                begrunnelse = row.stringOrNull("begrunnelse"),
            )
        }
    }

    fun getForDeltaker(deltakerId: UUID) = Database.query {
        val query = queryOf(
            """
            SELECT *
            FROM vurdering
            WHERE deltaker_id = :deltaker_id;
            """.trimIndent(),
            mapOf("deltaker_id" to deltakerId),
        )
        it.run(query.map(Companion::rowMapper).asList)
    }

    fun get(id: UUID) = Database.query {
        val query = queryOf(
            """
            SELECT *
            FROM vurdering 
            WHERE id = :id
            """.trimIndent(),
            mapOf("id" to id),
        ).map(Companion::rowMapper).asSingle
        it.run(query)?.let { d -> Result.success(d) }
            ?: Result.failure(NoSuchElementException("Ingen vurderinger med id $id"))
    }

    fun upsert(vurdering: Vurdering) = Database.query {
        val sql =
            """
            INSERT INTO vurdering(
                id,
                deltaker_id,
                opprettet_av_arrangor_ansatt_id,
                opprettet,
                begrunnelse,
                vurderingstype
            )
            VALUES (
                :id,
                :deltaker_id,
                :opprettet_av_arrangor_ansatt_id,
                :opprettet,
                :begrunnelse,
                :vurderingstype
            )
            ON CONFLICT (id) DO UPDATE SET
            		opprettet = :opprettet,
            		begrunnelse	= :begrunnelse,
                    vurderingstype = :vurderingstype,
                    modified_at = current_timestamp
            """.trimIndent()

        it.update(
            queryOf(
                sql,
                mapOf(
                    "id" to vurdering.id,
                    "deltaker_id" to vurdering.deltakerId,
                    "opprettet_av_arrangor_ansatt_id" to vurdering.opprettetAvArrangorAnsattId,
                    "opprettet" to vurdering.opprettet,
                    "begrunnelse" to vurdering.begrunnelse,
                    "vurderingstype" to vurdering.vurderingstype.name,
                ),
            ),
        )
    }
}
