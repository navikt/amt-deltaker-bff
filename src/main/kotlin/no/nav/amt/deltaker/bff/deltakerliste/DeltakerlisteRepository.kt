package no.nav.amt.deltaker.bff.deltakerliste

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.Database
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerlisteRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun rowMapper(row: Row) = Deltakerliste(
        id = row.uuid("id"),
        arrangorId = row.uuid("arrangor_id"),
        tiltak = Tiltak(
            navn = row.string("tiltaksnavn"),
            type = Tiltak.Type.valueOf(row.string("tiltakstype")),
        ),
        navn = row.string("navn"),
        status = Deltakerliste.Status.valueOf(row.string("status")),
        startDato = row.localDate("start_dato"),
        sluttDato = row.localDate("slutt_dato"),
        oppstart = Deltakerliste.Oppstartstype.valueOf(row.string("oppstart")),
    )

    fun upsert(deltakerliste: Deltakerliste) = Database.query {
        val sql = """
        INSERT INTO deltakerliste(
            id, 
            navn, 
            status, 
            arrangor_id, 
            tiltaksnavn, 
            tiltakstype, 
            start_dato, 
            slutt_dato, 
            oppstart)
        VALUES (:id,
        		:navn,
        		:status,
        		:arrangor_id,
        		:tiltaksnavn,
        		:tiltakstype,
        		:start_dato,
        		:slutt_dato,
                :oppstart)
        ON CONFLICT (id) DO UPDATE SET
        		navn     				= :navn,
        		status					= :status,
        		arrangor_id 			= :arrangor_id,
        		tiltaksnavn				= :tiltaksnavn,
        		tiltakstype				= :tiltakstype,
        		start_dato				= :start_dato,
        		slutt_dato				= :slutt_dato,
                oppstart                = :oppstart,
                modified_at             = current_timestamp
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

        log.info("Upsertet deltakerliste med id ${deltakerliste.id}")
    }

    fun delete(id: UUID) = Database.query {
        it.update(
            queryOf(
                statement = "delete from deltakerliste where id = :id",
                paramMap = mapOf("id" to id),
            ),
        )
        log.info("Slettet deltakerliste med id $id")
    }

    fun get(id: UUID) = Database.query {
        val query = queryOf(
            "select * from deltakerliste where id = :id",
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        it.run(query)
    }
}
