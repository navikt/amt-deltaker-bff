package no.nav.amt.deltaker.bff.auth

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Tiltakskoordinator
import no.nav.amt.lib.utils.database.Database
import java.time.LocalDate
import java.util.UUID

class TiltakskoordinatorTilgangRepository {
    private fun rowmapper(row: Row): TiltakskoordinatorDeltakerlisteTilgang = TiltakskoordinatorDeltakerlisteTilgang(
        id = row.uuid("id"),
        navAnsattId = row.uuid("nav_ansatt_id"),
        deltakerlisteId = row.uuid("deltakerliste_id"),
        gyldigFra = row.localDateTime("gyldig_fra"),
        gyldigTil = row.localDateTimeOrNull("gyldig_til"),
    )

    fun upsert(tilgang: TiltakskoordinatorDeltakerlisteTilgang) = Database.query {
        val sql =
            """
            insert into tiltakskoordinator_deltakerliste_tilgang 
                (id, nav_ansatt_id, deltakerliste_id, gyldig_fra, gyldig_til)
            values (:id, :nav_ansatt_id, :deltakerliste_id, :gyldig_fra, :gyldig_til)
            on conflict (id) do update set id               = :id,
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

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { deltakerlisteTilgang -> Result.success(deltakerlisteTilgang) }
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

    fun hentKoordinatorer(deltakerlisteId: UUID) = Database.query { session ->
        val sql =
            """
            SELECT DISTINCT ON (nav_ansatt.id)
                nav_ansatt.id,
                nav_ansatt.navn,
                (tilgang.gyldig_til IS NULL) AS er_aktiv
            FROM 
                tiltakskoordinator_deltakerliste_tilgang tilgang
                JOIN nav_ansatt ON nav_ansatt.id = tilgang.nav_ansatt_id
            WHERE tilgang.deltakerliste_id = :deltakerliste_id
            ORDER BY 
                nav_ansatt.id, 
                tilgang.gyldig_til DESC NULLS FIRST;
            """.trimIndent()

        val query = queryOf(
            statement = sql,
            paramMap = mapOf("deltakerliste_id" to deltakerlisteId),
        ).map {
            Tiltakskoordinator(
                id = it.uuid("id"),
                navn = it.string("navn"),
                erAktiv = it.boolean("er_aktiv"),
            )
        }.asList
        session.run(query)
    }

    fun get(id: UUID) = Database.query {
        val sql =
            """
            select *
            from tiltakskoordinator_deltakerliste_tilgang
            where id = :id
            """.trimIndent()
        val params = mapOf(
            "id" to id,
        )

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { tilgang ->
            Result.success(tilgang)
        } ?: Result.failure(
            NoSuchElementException("Fant ikke tilgang $id"),
        )
    }

    fun hentUtdaterteTilganger() = Database.query {
        val grense = LocalDate.now().minus(DeltakerlisteService.tiltakskoordinatorGraceperiode)
        val sql =
            """
            select
                t.id,
                t.nav_ansatt_id,
                t.deltakerliste_id,
                t.gyldig_fra,
                t.gyldig_til
            from tiltakskoordinator_deltakerliste_tilgang t 
                join deltakerliste dl on t.deltakerliste_id = dl.id
            where t.gyldig_til is null and dl.slutt_dato < :grense 
            """.trimIndent()

        val params = mapOf("grense" to grense)

        it.run(queryOf(sql, params).map(::rowmapper).asList)
    }

    fun hentAktiveForDeltakerliste(deltakerlisteId: UUID) = Database.query {
        val sql =
            """
            select
                t.id,
                t.nav_ansatt_id,
                t.deltakerliste_id,
                t.gyldig_fra,
                t.gyldig_til
            from tiltakskoordinator_deltakerliste_tilgang t 
                join deltakerliste dl on t.deltakerliste_id = dl.id
            where dl.id = :deltakerliste_id and t.gyldig_til is null 
            """
        val params = mapOf("deltakerliste_id" to deltakerlisteId)
        it.run(queryOf(sql, params).map(::rowmapper).asList)
    }
}
