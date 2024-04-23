package no.nav.amt.deltaker.bff.deltakerliste

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerlisteRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun rowMapper(row: Row) = Deltakerliste(
        id = row.uuid("deltakerliste_id"),
        tiltak = TiltakstypeRepository.rowMapper(row, "t"),
        navn = row.string("deltakerliste_navn"),
        status = Deltakerliste.Status.valueOf(row.string("status")),
        startDato = row.localDate("start_dato"),
        sluttDato = row.localDate("slutt_dato"),
        oppstart = Deltakerliste.Oppstartstype.valueOf(row.string("oppstart")),
        arrangor = Deltakerliste.Arrangor(
            arrangor = Arrangor(
                id = row.uuid("arrangor_id"),
                navn = row.string("arrangor_navn"),
                organisasjonsnummer = row.string("a.organisasjonsnummer"),
                overordnetArrangorId = row.uuidOrNull("a.overordnet_arrangor_id"),
            ),
            overordnetArrangorNavn = row.uuidOrNull("a.overordnet_arrangor_id")?.let {
                row.string("overordnet_arrangor_navn")
            },
        ),
    )

    fun upsert(deltakerliste: Deltakerliste) = Database.query {
        val sql =
            """
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
                    "arrangor_id" to deltakerliste.arrangor.arrangor.id,
                    "tiltaksnavn" to deltakerliste.tiltak.navn,
                    "tiltakstype" to deltakerliste.tiltak.arenaKode.name,
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
            """
            SELECT d.id   AS deltakerliste_id,
                   arrangor_id,
                   tiltaksnavn,
                   tiltakstype,
                   d.navn AS deltakerliste_navn,
                   status,
                   start_dato,
                   slutt_dato,
                   oppstart,
                   a.navn             AS arrangor_navn,
                   a.organisasjonsnummer as "a.organisasjonsnummer",
                   a.overordnet_arrangor_id as "a.overordnet_arrangor_id",
                   oa.navn as overordnet_arrangor_navn,
                   t.id as "t.id",
                   t.navn as "t.navn",
                   t.tiltakskode as "t.tiltakskode",
                   t.type as "t.type",
                   t.innsatsgrupper as "t.innsatsgrupper",
                   t.innhold as "t.innhold"
            FROM deltakerliste d
                     JOIN arrangor a ON a.id = d.arrangor_id
                     LEFT JOIN arrangor oa ON oa.id = a.overordnet_arrangor_id
                     LEFT JOIN tiltakstype t ON d.tiltakstype = t.type
            WHERE d.id = :id
            """.trimIndent(),
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        it.run(query)?.let { dl -> Result.success(dl) }
            ?: Result.failure(NoSuchElementException("Fant ikke deltakerliste med id $id"))
    }
}
