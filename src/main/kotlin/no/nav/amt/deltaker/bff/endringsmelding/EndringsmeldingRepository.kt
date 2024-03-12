package no.nav.amt.deltaker.bff.endringsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import java.util.UUID

class EndringsmeldingRepository {
    fun rowMapper(row: Row) = Endringsmelding(
        id = row.uuid("id"),
        deltakerId = row.uuid("deltaker_id"),
        utfortAvNavAnsattId = row.uuidOrNull("utfort_av_nav_ansatt_id"),
        opprettetAvArrangorAnsattId = row.uuid("opprettet_av_arrangor_ansatt_id"),
        utfortTidspunkt = row.localDateTimeOrNull("utfort_tidspunkt"),
        status = Endringsmelding.Status.valueOf(row.string("status")),
        type = Endringsmelding.Type.valueOf(row.string("type")),
        innhold = objectMapper.readValue(row.string("innhold")),
        createdAt = row.localDateTime("created_at"),
    )

    fun upsert(endringsmelding: Endringsmelding) = Database.query {
        val sql =
            """
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
            on conflict (id) do update set 
               id                              = :id,
               deltaker_id                     = :deltaker_id,
               utfort_av_nav_ansatt_id         = :utfort_av_nav_ansatt_id,
               opprettet_av_arrangor_ansatt_id = :opprettet_av_arrangor_ansatt_id,
               utfort_tidspunkt                = :utfort_tidspunkt,
               status                          = :status,
               type                            = :type,
               innhold                         = :innhold,
               modified_at                     = current_timestamp
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

    fun get(id: UUID) = Database.query {
        val query = queryOf(
            "select * from endringsmelding where id = :id",
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        it.run(query)?.let { endringsmelding -> Result.success(endringsmelding) }
            ?: Result.failure(NoSuchElementException("Ingen endringsmelding med id $id"))
    }

    fun delete(id: UUID) = Database.query {
        it.update(
            queryOf(
                "delete from endringsmelding where id = :id",
                mapOf("id" to id),
            ),
        )
    }
}
