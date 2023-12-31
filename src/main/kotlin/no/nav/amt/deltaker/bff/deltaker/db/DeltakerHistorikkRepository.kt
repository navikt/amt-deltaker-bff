package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerHistorikk
import java.util.UUID

class DeltakerHistorikkRepository {
    private fun rowMapper(row: Row): DeltakerHistorikk {
        val endringType = DeltakerEndringType.valueOf(row.string("endringstype"))
        return DeltakerHistorikk(
            id = row.uuid("id"),
            deltakerId = row.uuid("deltaker_id"),
            endringType = endringType,
            endring = parseDeltakerEndringJson(row.string("endring"), endringType),
            endretAv = row.stringOrNull("na.navn") ?: row.string("endret_av"),
            endretAvEnhet = row.stringOrNull("ne.navn") ?: row.stringOrNull("endret_av_enhet"),
            endret = row.localDateTime("dh.modified_at"),
        )
    }

    fun upsert(deltakerHistorikk: DeltakerHistorikk) = Database.query {
        val sql = """
            insert into deltaker_historikk (id, deltaker_id, endringstype, endring, endret_av, endret_av_enhet)
            values (:id, :deltaker_id, :endringstype, :endring, :endret_av, :endret_av_enhet)
            on conflict (id) do update set 
                deltaker_id = :deltaker_id,
                endringstype = :endringstype,
                endring = :endring,
                endret_av = :endret_av,
                endret_av_enhet = :endret_av_enhet,
                modified_at = current_timestamp
        """.trimIndent()

        val params = mapOf(
            "id" to deltakerHistorikk.id,
            "deltaker_id" to deltakerHistorikk.deltakerId,
            "endringstype" to deltakerHistorikk.endringType.name,
            "endring" to toPGObject(deltakerHistorikk.endring),
            "endret_av" to deltakerHistorikk.endretAv,
            "endret_av_enhet" to deltakerHistorikk.endretAvEnhet,
        )

        it.update(queryOf(sql, params))
    }

    fun getForDeltaker(deltakerId: UUID) = Database.query {
        val query = queryOf(
            """
                SELECT dh.id              AS id,
                       dh.deltaker_id     AS deltaker_id,
                       dh.endringstype    AS endringstype,
                       dh.endring         AS endring,
                       dh.endret_av       AS endret_av,
                       dh.endret_av_enhet AS endret_av_enhet,
                       dh.modified_at     AS "dh.modified_at",
                       na.navn            AS "na.navn",
                       ne.navn            AS "ne.navn"
                FROM deltaker_historikk dh
                         LEFT JOIN nav_ansatt na ON dh.endret_av = na.nav_ident
                         LEFT JOIN nav_enhet ne ON dh.endret_av_enhet = ne.nav_enhet_nummer
                WHERE deltaker_id = :deltaker_id
                ORDER BY dh.created_at;
            """.trimIndent(),
            mapOf("deltaker_id" to deltakerId),
        )
        it.run(query.map(::rowMapper).asList)
    }

    private fun parseDeltakerEndringJson(endringJson: String, endringType: DeltakerEndringType): DeltakerEndring {
        return when (endringType) {
            DeltakerEndringType.BAKGRUNNSINFORMASJON ->
                objectMapper.readValue<DeltakerEndring.EndreBakgrunnsinformasjon>(endringJson)
            DeltakerEndringType.MAL ->
                objectMapper.readValue<DeltakerEndring.EndreMal>(endringJson)
            DeltakerEndringType.DELTAKELSESMENGDE ->
                objectMapper.readValue<DeltakerEndring.EndreDeltakelsesmengde>(endringJson)
            DeltakerEndringType.STARTDATO ->
                objectMapper.readValue<DeltakerEndring.EndreStartdato>(endringJson)
            DeltakerEndringType.SLUTTDATO ->
                objectMapper.readValue<DeltakerEndring.EndreSluttdato>(endringJson)
        }
    }
}
