package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
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
            tiltak = Tiltak(
                navn = row.string("tiltaksnavn"),
                type = Tiltak.Type.valueOf(row.string("tiltakstype")),
            ),
            navn = row.string("deltakerliste_navn"),
            status = Deltakerliste.Status.valueOf(row.string("status")),
            startDato = row.localDate("start_dato"),
            sluttDato = row.localDate("slutt_dato"),
            oppstart = Deltakerliste.Oppstartstype.valueOf(row.string("oppstart")),
            arrangor = Arrangor(
                id = row.uuid("arrangor_id"),
                navn = row.string("arrangor_navn"),
                organisasjonsnummer = row.string("organisasjonsnummer"),
                overordnetArrangorId = row.uuidOrNull("overordnet_arrangor_id"),
            ),
        ),
        startdato = row.localDateOrNull("d.startdato"),
        sluttdato = row.localDateOrNull("d.sluttdato"),
        dagerPerUke = row.floatOrNull("d.dager_per_uke"),
        deltakelsesprosent = row.floatOrNull("d.deltakelsesprosent"),
        bakgrunnsinformasjon = row.stringOrNull("d.bakgrunnsinformasjon"),
        innhold = row.string("d.innhold").let { objectMapper.readValue(it) },
        status = DeltakerStatus(
            id = row.uuid("ds.id"),
            type = row.string("ds.type").let { DeltakerStatus.Type.valueOf(it) },
            aarsak = row.stringOrNull("ds.aarsak")?.let { objectMapper.readValue(it) },
            gyldigFra = row.localDateTime("ds.gyldig_fra"),
            gyldigTil = row.localDateTimeOrNull("ds.gyldig_til"),
            opprettet = row.localDateTime("ds.created_at"),
        ),
        vedtaksinformasjon = row.localDateTimeOrNull("sam.opprettet")?.let { opprettet ->
            Deltaker.Vedtaksinformasjon(
                fattet = row.localDateTimeOrNull("sam.godkjent"),
                fattetAvNav = row.stringOrNull("sam.godkjent_av_nav")?.let { g -> objectMapper.readValue(g) },
                opprettet = opprettet,
                opprettetAv = row.stringOrNull("sam.opprettet_av_navn") ?: row.string("sam.opprettet_av"),
                sistEndret = row.localDateTime("sam.sist_endret"),
                sistEndretAv = row.stringOrNull("sam.sist_endret_av_navn") ?: row.string("sam.sist_endret_av"),
            )
        },
        sistEndretAv = row.stringOrNull("na.navn") ?: row.string("d.sist_endret_av"),
        sistEndretAvEnhet = row.stringOrNull("d.sist_endret_av_enhet"),
        sistEndret = row.localDateTime("d.modified_at"),
        opprettet = row.localDateTime("d.created_at"),
    )

    fun upsert(deltaker: Deltaker) = Database.query { session ->
        val sql = """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold, sist_endret_av, sist_endret_av_enhet, modified_at
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold, :sistEndretAv, :sistEndretAvEnhet, :sistEndret
            )
            on conflict (id) do update set 
                person_id          = :person_id,
                startdato            = :startdato,
                sluttdato            = :sluttdato,
                dager_per_uke        = :dagerPerUke,
                deltakelsesprosent   = :deltakelsesprosent,
                bakgrunnsinformasjon = :bakgrunnsinformasjon,
                innhold              = :innhold,
                sist_endret_av       = :sistEndretAv,
                sist_endret_av_enhet = :sistEndretAvEnhet,
                modified_at          = :sistEndret
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
            "sistEndretAv" to deltaker.sistEndretAv,
            "sistEndretAvEnhet" to deltaker.sistEndretAvEnhet,
            "sistEndret" to deltaker.sistEndret,
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
                   d.sist_endret_av as "d.sist_endret_av",
                   d.sist_endret_av_enhet as "d.sist_endret_av_enhet",
                   d.created_at as "d.created_at",
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
                   na.navn as "na.navn",
                   ne.navn as "ne.navn",
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
                   organisasjonsnummer,
                   overordnet_arrangor_id,
                   sam.godkjent as "sam.godkjent",
                   sam.godkjent_av_nav as "sam.godkjent_av_nav",
                   sam.created_at as "sam.opprettet",
                   sam.opprettet_av as "sam.opprettet_av",
                   sam.modified_at as "sam.sist_endret",
                   sam.sist_endret_av as "sam.sist_endret_av",
                   na2.navn as "sam.opprettet_av_navn",
                   na3.navn as "sam.sist_endret_av_navn"
            from deltaker d 
                join nav_bruker nb on d.person_id = nb.person_id
                join deltaker_status ds on d.id = ds.deltaker_id
                join deltakerliste dl on d.deltakerliste_id = dl.id
                join arrangor a on a.id = dl.arrangor_id
                left join nav_ansatt na on d.sist_endret_av = na.nav_ident
                left join nav_enhet ne on d.sist_endret_av_enhet = ne.nav_enhet_nummer
                left join deltaker_samtykke sam on d.id = sam.deltaker_id and sam.gyldig_til is null
                left join nav_ansatt na2 on sam.opprettet_av = na2.nav_ident
                left join nav_ansatt na3 on sam.sist_endret_av = na2.nav_ident
      """ + "\n" + where
}
