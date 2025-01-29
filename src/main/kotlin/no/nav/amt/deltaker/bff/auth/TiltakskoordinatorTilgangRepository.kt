package no.nav.amt.deltaker.bff.auth

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.utils.prefixColumn
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

class TiltakskoordinatorTilgangRepository {
    private fun rowmapper(row: Row, alias: String = "tdt"): TiltakskoordinatorDeltakerlisteTilgang {
        val col = prefixColumn(alias)

        return TiltakskoordinatorDeltakerlisteTilgang(
            id = row.uuid(col("id")),
            navAnsattId = row.uuid(col("nav_ansatt_id")),
            deltakerlisteId = row.uuid(col("deltakerliste_id")),
            gyldigFra = row.localDateTime(col("gyldig_fra")),
            gyldigTil = row.localDateTimeOrNull(col("gyldig_til")),
        )
    }

    fun upsert(tilgang: TiltakskoordinatorDeltakerlisteTilgang) = Database.query {
        val sql =
            """
            insert into tiltakskoordinator_deltakerliste_tilgang 
                (id, nav_ansatt_id, deltakerliste_id, gyldig_fra, gyldig_til)
            values (:id, :nav_ansatt_id, :deltakerliste_id, :gyldig_fra, :gyldig_til)
            on conflict do update set id               = :id,
                                      nav_ansatt_id    = :nav_ansatt_id,
                                      deltakerliste_id = :deltakerliste_id,
                                      gyldig_fra       = :gyldig_fra,
                                      gyldig_til       = :gyldig_til,
                                      modified_at      = current_timestamp
            returning *
            """.trimIndent()
        val params = mapOf(
            "id" to tilgang.id,
            "nav_ansatt_id" to tilgang.navAnsattId,
            "deltakerliste_id" to tilgang.deltakerlisteId,
            "gyldig_fra" to tilgang.gyldigFra,
            "gyldig_til" to tilgang.gyldigTil,
        )

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Noe gikk galt med upsert av tiltakskoordinator tilgang med id ${tilgang.id}"))
    }

    fun hentAktivTilgang(navAnsattId: UUID, deltakerlisteId: UUID) = Database.query {
        val sql =
            """
            select *
            from tiltakskoordinator_deltakerliste_tilgang
            where nav_ansatt_id = :nav_ansatt_id
              and deltakerliste_id = :deltakerliste_id
              and gyldig_til is null
            """.trimIndent()
        val params = mapOf(
            "nav_ansatt_id" to navAnsattId,
            "deltakerliste_id" to deltakerlisteId,
        )

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { tilgang ->
            Result.success(tilgang)
        } ?: Result.failure(
            NoSuchElementException("Fant ikke aktiv tilgang for nav-ansatt: $navAnsattId og deltakerliste: $deltakerlisteId"),
        )
    }
}
