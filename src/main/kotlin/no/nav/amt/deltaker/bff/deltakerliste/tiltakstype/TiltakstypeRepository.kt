package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import org.slf4j.LoggerFactory

class TiltakstypeRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun rowMapper(row: Row) = Tiltakstype(
        id = row.uuid("id"),
        navn = row.string("navn"),
        arenaKode = Tiltakstype.ArenaKode.valueOf(row.string("type")),
        innhold = row.stringOrNull("innhold")?.let { objectMapper.readValue(it) },
    )

    fun upsert(tiltakstype: Tiltakstype) = Database.query {
        val sql =
            """
            INSERT INTO tiltakstype(
                id, 
                navn, 
                type, 
                innhold)
            VALUES (:id,
            		:navn,
            		:type,
            		:innhold)
            ON CONFLICT (id) DO UPDATE SET
            		navn     		    = :navn,
            		type				= :type,
            		innhold 			= :innhold,
                    modified_at         = current_timestamp
            """.trimIndent()

        it.update(
            queryOf(
                sql,
                mapOf(
                    "id" to tiltakstype.id,
                    "navn" to tiltakstype.navn,
                    "type" to tiltakstype.arenaKode.name,
                    "innhold" to toPGObject(tiltakstype.innhold),
                ),
            ),
        )

        log.info("Upsertet tiltakstype med id ${tiltakstype.id}")
    }

    fun get(kode: Tiltakstype.ArenaKode) = Database.query {
        val query = queryOf(
            """
            SELECT id,
               navn,
               type,
               innhold
            FROM tiltakstype
            WHERE type = :type
            """.trimIndent(),
            mapOf("type" to kode.name),
        ).map(::rowMapper).asSingle

        it.run(query)?.let { t -> Result.success(t) }
            ?: Result.failure(NoSuchElementException("Fant ikke tiltakstype ${kode.name}"))
    }
}
