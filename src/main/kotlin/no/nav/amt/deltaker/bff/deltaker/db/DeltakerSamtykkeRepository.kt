package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerSamtykke
import java.util.UUID

class DeltakerSamtykkeRepository {
    fun rowMapper(row: Row) = DeltakerSamtykke(
        id = row.uuid("id"),
        deltakerId = row.uuid("deltaker_id"),
        godkjent = row.localDateTimeOrNull("godkjent"),
        gyldigTil = row.localDateTimeOrNull("gyldig_til"),
        deltakerVedSamtykke = objectMapper.readValue(row.string("deltaker_ved_samtykke")),
        godkjentAvNav = row.stringOrNull("godkjent_av_nav")?.let { objectMapper.readValue(it) },
        opprettet = row.localDateTime("created_at"),
        opprettetAv = row.string("opprettet_av"),
        opprettetAvEnhet = row.stringOrNull("opprettet_av_enhet"),
    )

    fun upsert(samtykke: DeltakerSamtykke) = Database.query {
        val sql = """
            insert into deltaker_samtykke (id, deltaker_id, godkjent, gyldig_til, deltaker_ved_samtykke, godkjent_av_nav, opprettet_av, opprettet_av_enhet)
            values (:id, :deltaker_id, :godkjent, :gyldig_til, :deltaker_ved_samtykke, :godkjent_av_nav, :opprettet_av, :opprettet_av_enhet)
            on conflict (id) do update set 
                godkjent = :godkjent,
                gyldig_til = :gyldig_til,
                deltaker_ved_samtykke = :deltaker_ved_samtykke,
                godkjent_av_nav = :godkjent_av_nav,
                modified_at = current_timestamp
        """.trimIndent()

        val params = mapOf(
            "id" to samtykke.id,
            "deltaker_id" to samtykke.deltakerId,
            "godkjent" to samtykke.godkjent,
            "gyldig_til" to samtykke.gyldigTil,
            "deltaker_ved_samtykke" to toPGObject(samtykke.deltakerVedSamtykke),
            "godkjent_av_nav" to samtykke.godkjentAvNav?.let(::toPGObject),
            "opprettet_av" to samtykke.opprettetAv,
            "opprettet_av_enhet" to samtykke.opprettetAvEnhet,
        )

        it.update(queryOf(sql, params))
    }

    fun get(id: UUID) = Database.query {
        val query = queryOf("select * from deltaker_samtykke where id = :id", mapOf("id" to id))

        it.run(query.map(::rowMapper).asSingle)
    }

    fun getIkkeGodkjent(deltakerId: UUID) = Database.query {
        val sql = """
            select * from deltaker_samtykke
            where deltaker_id = :deltaker_id and godkjent is null
        """.trimIndent()

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId))

        it.run(query.map(::rowMapper).asSingle)
    }

    fun getForDeltaker(deltakerId: UUID) = Database.query {
        val query = queryOf(
            "select * from deltaker_samtykke where deltaker_id = :deltaker_id order by created_at",
            mapOf("deltaker_id" to deltakerId),
        )
        it.run(query.map(::rowMapper).asList)
    }
}
