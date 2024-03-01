package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.AVSLUTTENDE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import java.util.UUID

class DeltakerRepository {
    fun rowMapper(row: Row) = Deltaker(
        id = row.uuid("d.id"),
        navBruker = NavBruker(
            personId = row.uuid("d.person_id"),
            personident = row.string("nb.personident"),
            fornavn = row.string("nb.fornavn"),
            mellomnavn = row.stringOrNull("nb.mellomnavn"),
            etternavn = row.string("nb.etternavn"),
        ),
        deltakerliste = Deltakerliste(
            id = row.uuid("deltakerliste_id"),
            tiltak = Tiltakstype(
                id = row.uuid("t.id"),
                navn = row.string("t.navn"),
                type = Tiltak.Type.valueOf(row.string("t.type")),
                innhold = row.stringOrNull("t.innhold")?.let {
                    objectMapper.readValue<DeltakerRegistreringInnhold?>(it)?.let { i ->
                        if (i.innholdselementer.isNotEmpty()) {
                            i.copy(innholdselementer = i.innholdselementer.plus(annetInnholdselement))
                        } else {
                            i
                        }
                    }
                },
            ),
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
        ),
        startdato = row.localDateOrNull("d.startdato"),
        sluttdato = row.localDateOrNull("d.sluttdato"),
        dagerPerUke = row.floatOrNull("d.dager_per_uke"),
        deltakelsesprosent = row.floatOrNull("d.deltakelsesprosent"),
        bakgrunnsinformasjon = row.stringOrNull("d.bakgrunnsinformasjon"),
        innhold = objectMapper.readValue(row.string("d.innhold")),
        status = DeltakerStatus(
            id = row.uuid("ds.id"),
            type = row.string("ds.type").let { DeltakerStatus.Type.valueOf(it) },
            aarsak = row.stringOrNull("ds.aarsak")?.let { objectMapper.readValue(it) },
            gyldigFra = row.localDateTime("ds.gyldig_fra"),
            gyldigTil = row.localDateTimeOrNull("ds.gyldig_til"),
            opprettet = row.localDateTime("ds.created_at"),
        ),
        vedtaksinformasjon = row.localDateTimeOrNull("v.opprettet")?.let { opprettet ->
            Deltaker.Vedtaksinformasjon(
                fattet = row.localDateTimeOrNull("v.fattet"),
                fattetAvNav = row.stringOrNull("v.fattet_av_nav")?.let { g -> objectMapper.readValue(g) },
                opprettet = opprettet,
                opprettetAv = row.string("v.opprettet_av"),
                sistEndret = row.localDateTime("v.sist_endret"),
                sistEndretAv = row.string("v.sist_endret_av"),
                sistEndretAvEnhet = row.stringOrNull("v.sist_endret_av_enhet"),
            )
        },
    )

    fun upsert(deltaker: Deltaker) = Database.query { session ->
        val sql = """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold
            )
            on conflict (id) do update set 
                person_id          = :person_id,
                startdato            = :startdato,
                sluttdato            = :sluttdato,
                dager_per_uke        = :dagerPerUke,
                deltakelsesprosent   = :deltakelsesprosent,
                bakgrunnsinformasjon = :bakgrunnsinformasjon,
                innhold              = :innhold,
                modified_at          = current_timestamp
        """.trimIndent()

        val parameters = mapOf(
            "id" to deltaker.id,
            "person_id" to deltaker.navBruker.personId,
            "deltakerlisteId" to deltaker.deltakerliste.id,
            "startdato" to deltaker.startdato,
            "sluttdato" to deltaker.sluttdato,
            "dagerPerUke" to deltaker.dagerPerUke,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
            "innhold" to toPGObject(deltaker.innhold),
        )

        session.transaction { tx ->
            tx.update(queryOf(sql, parameters))
            tx.update(insertStatusQuery(deltaker.status, deltaker.id))
            tx.update(deaktiverTidligereStatuserQuery(deltaker.status, deltaker.id))
        }
    }

    fun get(id: UUID) = Database.query {
        val sql = getDeltakerSql("where d.id = :id and ds.gyldig_til is null")

        val query = queryOf(sql, mapOf("id" to id)).map(::rowMapper).asSingle
        it.run(query)?.let { d -> Result.success(d) }
            ?: Result.failure(NoSuchElementException("Ingen deltaker med id $id"))
    }

    fun getMany(personIdent: String, deltakerlisteId: UUID) = Database.query {
        val sql = getDeltakerSql(
            """ where nb.personident = :personident 
                    and d.deltakerliste_id = :deltakerliste_id 
                    and ds.gyldig_til is null
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "personident" to personIdent,
                "deltakerliste_id" to deltakerlisteId,
            ),
        ).map(::rowMapper).asList
        it.run(query)
    }

    fun getDeltakerStatuser(deltakerId: UUID) = Database.query { session ->
        val sql = """
            select * from deltaker_status where deltaker_id = :deltaker_id
        """.trimIndent()

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId)).map {
            DeltakerStatus(
                id = it.uuid("id"),
                type = it.string("type").let { t -> DeltakerStatus.Type.valueOf(t) },
                aarsak = it.stringOrNull("aarsak")?.let { aarsak -> objectMapper.readValue(aarsak) },
                gyldigFra = it.localDateTime("gyldig_fra"),
                gyldigTil = it.localDateTimeOrNull("gyldig_til"),
                opprettet = it.localDateTime("created_at"),
            )
        }
            .asList

        session.run(query)
    }

    fun delete(deltakerId: UUID) = Database.query { session ->
        session.transaction { tx ->
            tx.update(slettStatus(deltakerId))
            tx.update(slettDeltaker(deltakerId))
        }
    }

    fun skalHaStatusDeltar(): List<Deltaker> = Database.query { session ->
        val sql = getDeltakerSql(
            """ where ds.gyldig_til is null
                and ds.type = :status
                and d.startdato <= CURRENT_DATE
                and (d.sluttdato is null or d.sluttdato >= CURRENT_DATE)
            """.trimMargin(),
        )

        val query = queryOf(sql, mapOf("status" to DeltakerStatus.Type.VENTER_PA_OPPSTART.name))
            .map(::rowMapper).asList

        session.run(query)
    }

    fun skalHaAvsluttendeStatus(): List<Deltaker> = Database.query { session ->
        val deltakerstatuser = listOf(
            DeltakerStatus.Type.VENTER_PA_OPPSTART.name,
            DeltakerStatus.Type.DELTAR.name,
        )

        val sql = getDeltakerSql(
            """ where ds.gyldig_til is null
                and ds.type in (${deltakerstatuser.joinToString { "?" }})
                and d.sluttdato < CURRENT_DATE
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            *deltakerstatuser.toTypedArray(),
        )
            .map(::rowMapper).asList

        session.run(query)
    }

    fun deltarPaAvsluttetDeltakerliste(): List<Deltaker> = Database.query { session ->
        val avsluttendeDeltakerStatuser = AVSLUTTENDE_STATUSER.map { it.name }
        val avsluttendeDeltakerlisteStatuser = listOf(
            Deltakerliste.Status.AVSLUTTET.name,
            Deltakerliste.Status.AVBRUTT.name,
            Deltakerliste.Status.AVLYST.name,
        )
        val sql = getDeltakerSql(
            """ where ds.gyldig_til is null
                and ds.type not in (${avsluttendeDeltakerStatuser.joinToString { "?" }})
                and dl.status in (${avsluttendeDeltakerlisteStatuser.joinToString { "?" }})
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            *avsluttendeDeltakerStatuser.toTypedArray(),
            *avsluttendeDeltakerlisteStatuser.toTypedArray(),
        )
            .map(::rowMapper).asList

        session.run(query)
    }

    private fun slettStatus(deltakerId: UUID): Query {
        val sql = """
            delete from deltaker_status
            where deltaker_id = :deltaker_id;
        """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
        )

        return queryOf(sql, params)
    }

    private fun slettDeltaker(deltakerId: UUID): Query {
        val sql = """
            delete from deltaker
            where id = :deltaker_id;
        """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
        )

        return queryOf(sql, params)
    }

    private fun insertStatusQuery(status: DeltakerStatus, deltakerId: UUID): Query {
        val sql = """
            insert into deltaker_status(id, deltaker_id, type, aarsak, gyldig_fra) 
            values (:id, :deltaker_id, :type, :aarsak, :gyldig_fra) 
            on conflict (id) do nothing;
        """.trimIndent()

        val params = mapOf(
            "id" to status.id,
            "deltaker_id" to deltakerId,
            "type" to status.type.name,
            "aarsak" to toPGObject(status.aarsak),
            "gyldig_fra" to status.gyldigFra,
        )

        return queryOf(sql, params)
    }

    private fun deaktiverTidligereStatuserQuery(status: DeltakerStatus, deltakerId: UUID): Query {
        val sql = """
            update deltaker_status
            set gyldig_til = current_timestamp
            where deltaker_id = :deltaker_id 
              and id != :id 
              and gyldig_til is null;
        """.trimIndent()

        return queryOf(sql, mapOf("id" to status.id, "deltaker_id" to deltakerId))
    }

    private fun getDeltakerSql(where: String = "") = """
            select d.id as "d.id",
                   d.person_id as "d.person_id",
                   d.deltakerliste_id as "d.deltakerliste_id",
                   d.startdato as "d.startdato",
                   d.sluttdato as "d.sluttdato",
                   d.dager_per_uke as "d.dager_per_uke",
                   d.deltakelsesprosent as "d.deltakelsesprosent",
                   d.bakgrunnsinformasjon as "d.bakgrunnsinformasjon",
                   d.innhold as "d.innhold",
                   d.modified_at as "d.modified_at",
                   nb.personident as "nb.personident",
                   nb.fornavn as "nb.fornavn",
                   nb.mellomnavn as "nb.mellomnavn",
                   nb.etternavn as "nb.etternavn",
                   ds.id as "ds.id",
                   ds.deltaker_id as "ds.deltaker_id",
                   ds.type as "ds.type",
                   ds.aarsak as "ds.aarsak",
                   ds.gyldig_fra as "ds.gyldig_fra",
                   ds.gyldig_til as "ds.gyldig_til",
                   ds.created_at as "ds.created_at",
                   ds.modified_at as "ds.modified_at",
                   dl.id as deltakerliste_id,
                   dl.arrangor_id,
                   dl.tiltaksnavn,
                   dl.tiltakstype,
                   dl.navn AS deltakerliste_navn,
                   dl.status,
                   dl.start_dato,
                   dl.slutt_dato,
                   dl.oppstart,
                   a.navn             AS arrangor_navn,
                   a.organisasjonsnummer as "a.organisasjonsnummer",
                   a.overordnet_arrangor_id as "a.overordnet_arrangor_id",
                   oa.navn  AS overordnet_arrangor_navn,
                   v.fattet as "v.fattet",
                   v.fattet_av_nav as "v.fattet_av_nav",
                   v.created_at as "v.opprettet",
                   v.opprettet_av as "v.opprettet_av",
                   v.modified_at as "v.sist_endret",
                   v.sist_endret_av as "v.sist_endret_av",
                   v.sist_endret_av_enhet as "v.sist_endret_av_enhet",
                   t.id as "t.id",
                   t.navn as "t.navn",
                   t.type as "t.type",
                   t.innhold as "t.innhold"
            from deltaker d 
                join nav_bruker nb on d.person_id = nb.person_id
                join deltaker_status ds on d.id = ds.deltaker_id
                join deltakerliste dl on d.deltakerliste_id = dl.id
                join arrangor a on a.id = dl.arrangor_id
                join tiltakstype t on t.type = dl.tiltakstype
                left join arrangor oa on oa.id = a.overordnet_arrangor_id
                left join vedtak v on d.id = v.deltaker_id and v.gyldig_til is null
                $where
      """
}
