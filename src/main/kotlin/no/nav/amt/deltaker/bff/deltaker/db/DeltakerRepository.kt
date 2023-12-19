package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import java.util.UUID

class DeltakerRepository {
    fun rowMapper(row: Row) = Deltaker(
        id = row.uuid("d.id"),
        personident = row.string("d.personident"),
        deltakerlisteId = row.uuid("d.deltakerliste_id"),
        startdato = row.localDateOrNull("d.startdato"),
        sluttdato = row.localDateOrNull("d.sluttdato"),
        dagerPerUke = row.floatOrNull("d.dager_per_uke"),
        deltakelsesprosent = row.floatOrNull("d.deltakelsesprosent"),
        bakgrunnsinformasjon = row.stringOrNull("d.bakgrunnsinformasjon"),
        mal = row.string("d.mal").let { objectMapper.readValue(it) },
        status = DeltakerStatus(
            id = row.uuid("ds.id"),
            type = row.string("ds.type").let { DeltakerStatus.Type.valueOf(it) },
            aarsak = row.stringOrNull("ds.aarsak")?.let { DeltakerStatus.Aarsak.valueOf(it) },
            gyldigFra = row.localDateTime("ds.gyldig_fra"),
            gyldigTil = row.localDateTimeOrNull("ds.gyldig_til"),
            opprettet = row.localDateTime("ds.created_at"),
        ),
        sistEndretAv = row.stringOrNull("na.navn") ?: row.string("d.sist_endret_av"),
        sistEndretAvEnhet = row.stringOrNull("ne.navn") ?: row.stringOrNull("d.sist_endret_av_enhet"),
        sistEndret = row.localDateTime("d.modified_at"),
        opprettet = row.localDateTime("d.created_at"),
    )

    fun upsert(deltaker: Deltaker) = Database.query { session ->
        val sql = """
            insert into deltaker(
                id, personident, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, mal, sist_endret_av, sist_endret_av_enhet, modified_at
            )
            values (
                :id, :personident, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :mal, :sistEndretAv, :sistEndretAvEnhet, :sistEndret
            )
            on conflict (id) do update set 
                personident          = :personident,
                startdato            = :startdato,
                sluttdato            = :sluttdato,
                dager_per_uke        = :dagerPerUke,
                deltakelsesprosent   = :deltakelsesprosent,
                bakgrunnsinformasjon = :bakgrunnsinformasjon,
                mal                  = :mal,
                sist_endret_av       = :sistEndretAv,
                sist_endret_av_enhet = :sistEndretAvEnhet,
                modified_at          = :sistEndret
        """.trimIndent()

        val parameters = mapOf(
            "id" to deltaker.id,
            "personident" to deltaker.personident,
            "deltakerlisteId" to deltaker.deltakerlisteId,
            "startdato" to deltaker.startdato,
            "sluttdato" to deltaker.sluttdato,
            "dagerPerUke" to deltaker.dagerPerUke,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
            "mal" to toPGObject(deltaker.mal),
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
        val sql = """
            select d.id as "d.id",
                   d.personident as "d.personident",
                   d.deltakerliste_id as "d.deltakerliste_id",
                   d.startdato as "d.startdato",
                   d.sluttdato as "d.sluttdato",
                   d.dager_per_uke as "d.dager_per_uke",
                   d.deltakelsesprosent as "d.deltakelsesprosent",
                   d.bakgrunnsinformasjon as "d.bakgrunnsinformasjon",
                   d.mal as "d.mal",
                   d.sist_endret_av as "d.sist_endret_av",
                   d.sist_endret_av_enhet as "d.sist_endret_av_enhet",
                   d.created_at as "d.created_at",
                   d.modified_at as "d.modified_at",
                   ds.id as "ds.id",
                   ds.deltaker_id as "ds.deltaker_id",
                   ds.type as "ds.type",
                   ds.aarsak as "ds.aarsak",
                   ds.gyldig_fra as "ds.gyldig_fra",
                   ds.gyldig_til as "ds.gyldig_til",
                   ds.created_at as "ds.created_at",
                   ds.modified_at as "ds.modified_at",
                   na.navn as "na.navn",
                   ne.navn as "ne.navn"
            from deltaker d 
                join deltaker_status ds on d.id = ds.deltaker_id
                left join nav_ansatt na on d.sist_endret_av = na.nav_ident
                left join nav_enhet ne on d.sist_endret_av_enhet = ne.nav_enhet_nummer
            where d.id = :id and ds.gyldig_til is null
        """.trimIndent()

        val query = queryOf(sql, mapOf("id" to id)).map(::rowMapper).asSingle
        it.run(query)
    }

    fun get(personIdent: String, deltakerlisteId: UUID): Deltaker? =
        Database.query {
            val sql = """
            select d.id as "d.id",
                   d.personident as "d.personident",
                   d.deltakerliste_id as "d.deltakerliste_id",
                   d.startdato as "d.startdato",
                   d.sluttdato as "d.sluttdato",
                   d.dager_per_uke as "d.dager_per_uke",
                   d.deltakelsesprosent as "d.deltakelsesprosent",
                   d.bakgrunnsinformasjon as "d.bakgrunnsinformasjon",
                   d.mal as "d.mal",
                   d.sist_endret_av as "d.sist_endret_av",
                   d.sist_endret_av_enhet as "d.sist_endret_av_enhet",
                   d.created_at as "d.created_at",
                   d.modified_at as "d.modified_at",
                   ds.id as "ds.id",
                   ds.deltaker_id as "ds.deltaker_id",
                   ds.type as "ds.type",
                   ds.aarsak as "ds.aarsak",
                   ds.gyldig_fra as "ds.gyldig_fra",
                   ds.gyldig_til as "ds.gyldig_til",
                   ds.created_at as "ds.created_at",
                   ds.modified_at as "ds.modified_at",
                   na.navn as "na.navn",
                   ne.navn as "ne.navn"
            from deltaker d 
                join deltaker_status ds on d.id = ds.deltaker_id
                left join nav_ansatt na on d.sist_endret_av = na.nav_ident
                left join nav_enhet ne on d.sist_endret_av_enhet = ne.nav_enhet_nummer
            where d.personident = :personident and d.deltakerliste_id = :deltakerliste_id and ds.gyldig_til is null
            """.trimIndent()

            val query = queryOf(
                sql,
                mapOf(
                    "personident" to personIdent,
                    "deltakerliste_id" to deltakerlisteId,
                ),
            ).map(::rowMapper).asSingle
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
                aarsak = it.stringOrNull("aarsak")?.let { t -> DeltakerStatus.Aarsak.valueOf(t) },
                gyldigFra = it.localDateTime("gyldig_fra"),
                gyldigTil = it.localDateTimeOrNull("gyldig_til"),
                opprettet = it.localDateTime("created_at"),
            )
        }
            .asList

        session.run(query)
    }

    fun slettKladd(deltakerId: UUID) = Database.query { session ->
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
            "aarsak" to status.aarsak?.name,
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
}
