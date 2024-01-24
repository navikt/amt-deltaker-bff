package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.Database
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import java.util.UUID

class DeltakerEndringRepository {
    private fun rowMapper(row: Row): DeltakerEndring {
        val endringstype = DeltakerEndring.Endringstype.valueOf(row.string("endringstype"))
        return DeltakerEndring(
            id = row.uuid("id"),
            deltakerId = row.uuid("deltaker_id"),
            endringstype = endringstype,
            endring = parseDeltakerEndringJson(row.string("endring"), endringstype),
            endretAv = row.stringOrNull("na.navn") ?: row.string("endret_av"),
            endretAvEnhet = row.stringOrNull("ne.navn") ?: row.stringOrNull("endret_av_enhet"),
            endret = row.localDateTime("dh.modified_at"),
        )
    }

    fun upsert(deltakerEndring: DeltakerEndring) = Database.query {
        val sql = """
            insert into deltaker_endring (id, deltaker_id, endringstype, endring, endret_av, endret_av_enhet)
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
            "id" to deltakerEndring.id,
            "deltaker_id" to deltakerEndring.deltakerId,
            "endringstype" to deltakerEndring.endringstype.name,
            "endring" to toPGObject(deltakerEndring.endring),
            "endret_av" to deltakerEndring.endretAv,
            "endret_av_enhet" to deltakerEndring.endretAvEnhet,
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
                FROM deltaker_endring dh
                         LEFT JOIN nav_ansatt na ON dh.endret_av = na.nav_ident
                         LEFT JOIN nav_enhet ne ON dh.endret_av_enhet = ne.nav_enhet_nummer
                WHERE deltaker_id = :deltaker_id
                ORDER BY dh.created_at;
            """.trimIndent(),
            mapOf("deltaker_id" to deltakerId),
        )
        it.run(query.map(::rowMapper).asList)
    }

    private fun parseDeltakerEndringJson(endringJson: String, endringType: DeltakerEndring.Endringstype): DeltakerEndring.Endring {
        return when (endringType) {
            DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON ->
                objectMapper.readValue<DeltakerEndring.Endring.EndreBakgrunnsinformasjon>(endringJson)

            DeltakerEndring.Endringstype.MAL ->
                objectMapper.readValue<DeltakerEndring.Endring.EndreMal>(endringJson)

            DeltakerEndring.Endringstype.DELTAKELSESMENGDE ->
                objectMapper.readValue<DeltakerEndring.Endring.EndreDeltakelsesmengde>(endringJson)

            DeltakerEndring.Endringstype.STARTDATO ->
                objectMapper.readValue<DeltakerEndring.Endring.EndreStartdato>(endringJson)

            DeltakerEndring.Endringstype.SLUTTDATO ->
                objectMapper.readValue<DeltakerEndring.Endring.EndreSluttdato>(endringJson)

            DeltakerEndring.Endringstype.IKKE_AKTUELL ->
                objectMapper.readValue<DeltakerEndring.Endring.IkkeAktuell>(endringJson)
        }
    }
}
