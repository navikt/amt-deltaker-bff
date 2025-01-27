package no.nav.amt.deltaker.bff.deltakerliste

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.utils.prefixColumn
import no.nav.amt.lib.utils.database.Database
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerlisteRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        fun rowMapper(row: Row, alias: String? = "dl"): Deltakerliste {
            val col = prefixColumn(alias)

            return Deltakerliste(
                id = row.uuid(col("id")),
                tiltak = TiltakstypeRepository.rowMapper(row, "t"),
                navn = row.string(col("navn")),
                status = Deltakerliste.Status.valueOf(row.string(col("status"))),
                startDato = row.localDate(col("start_dato")),
                sluttDato = row.localDateOrNull(col("slutt_dato")),
                oppstart = Deltakerliste.Oppstartstype.valueOf(row.string(col("oppstart"))),
                arrangor = Deltakerliste.Arrangor(
                    arrangor = Arrangor(
                        id = row.uuid("a.id"),
                        navn = row.string("a.navn"),
                        organisasjonsnummer = row.string("a.organisasjonsnummer"),
                        overordnetArrangorId = row.uuidOrNull("a.overordnet_arrangor_id"),
                    ),
                    overordnetArrangorNavn = row.uuidOrNull("a.overordnet_arrangor_id")?.let {
                        row.string("oa.navn")
                    },
                ),
                antallPlasser = row.intOrNull(col("antall_plasser")),
                apentForPamelding = row.boolean(col("apent_for_pamelding")),
            )
        }
    }

    fun upsert(deltakerliste: Deltakerliste) = Database.query {
        val sql =
            """
            INSERT INTO deltakerliste(
                id, 
                navn, 
                status, 
                arrangor_id, 
                tiltakstype_id, 
                start_dato, 
                slutt_dato, 
                oppstart,
                apent_for_pamelding,
                antall_plasser)
            VALUES (:id,
            		:navn,
            		:status,
            		:arrangor_id,
            		:tiltakstype_id,
            		:start_dato,
            		:slutt_dato,
                    :oppstart,
                    :apent_for_pamelding,
                    :antall_plasser)
            ON CONFLICT (id) DO UPDATE SET
            		navn     				= :navn,
            		status					= :status,
            		arrangor_id 			= :arrangor_id,
            		tiltakstype_id			= :tiltakstype_id,
            		start_dato				= :start_dato,
            		slutt_dato				= :slutt_dato,
                    oppstart                = :oppstart,
                    modified_at             = current_timestamp,
                    apent_for_pamelding     = :apent_for_pamelding,
                    antall_plasser          = :antall_plasser
            """.trimIndent()

        it.update(
            queryOf(
                sql,
                mapOf(
                    "id" to deltakerliste.id,
                    "navn" to deltakerliste.navn,
                    "status" to deltakerliste.status.name,
                    "arrangor_id" to deltakerliste.arrangor.arrangor.id,
                    "tiltakstype_id" to deltakerliste.tiltak.id,
                    "start_dato" to deltakerliste.startDato,
                    "slutt_dato" to deltakerliste.sluttDato,
                    "oppstart" to deltakerliste.oppstart?.name,
                    "apent_for_pamelding" to deltakerliste.apentForPamelding,
                    "antall_plasser" to deltakerliste.antallPlasser,
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
            SELECT 
                   dl.id as "dl.id",
                   dl.navn as "dl.navn",
                   dl.status as "dl.status",
                   dl.start_dato as "dl.start_dato",
                   dl.slutt_dato as "dl.slutt_dato",
                   dl.oppstart as "dl.oppstart",
                   dl.apent_for_pamelding as "dl.apent_for_pamelding",
                   dl.antall_plasser as "dl.antall_plasser",
                   a.id as "a.id",
                   a.navn as "a.navn",
                   a.organisasjonsnummer as "a.organisasjonsnummer",
                   a.overordnet_arrangor_id as "a.overordnet_arrangor_id",
                   oa.navn as "oa.navn",
                   t.id as "t.id",
                   t.navn as "t.navn",
                   t.tiltakskode as "t.tiltakskode",
                   t.type as "t.type",
                   t.innsatsgrupper as "t.innsatsgrupper",
                   t.innhold as "t.innhold"
            FROM deltakerliste dl
                     JOIN arrangor a ON a.id = dl.arrangor_id
                     LEFT JOIN arrangor oa ON oa.id = a.overordnet_arrangor_id
                     LEFT JOIN tiltakstype t ON dl.tiltakstype_id = t.id
            WHERE dl.id = :id
            """.trimIndent(),
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        it.run(query)?.let { dl -> Result.success(dl) }
            ?: Result.failure(NoSuchElementException("Fant ikke deltakerliste med id $id"))
    }
}
