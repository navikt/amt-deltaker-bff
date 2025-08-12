package no.nav.amt.deltaker.bff.navenhet

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.models.person.NavEnhet
import no.nav.amt.lib.utils.database.Database
import java.time.LocalDateTime
import java.util.UUID

class NavEnhetRepository {
    private fun rowMapper(row: Row) = NavEnhetDbo(
        id = row.uuid("id"),
        enhetsnummer = row.string("nav_enhet_nummer"),
        navn = row.string("navn"),
        sistEndret = row.localDateTime("modified_at"),
    )

    fun upsert(navEnhet: NavEnhet): NavEnhetDbo {
        val sql =
            """
            INSERT INTO nav_enhet(id, nav_enhet_nummer, navn, modified_at)
            VALUES (:id, :nav_enhet_nummer, :navn, :modified_at) 
            ON CONFLICT (id) DO UPDATE SET
                nav_enhet_nummer = :nav_enhet_nummer,
                navn = :navn,
                modified_at = :modified_at
            returning *
            """.trimIndent()

        return Database.query {
            val query = queryOf(
                sql,
                mapOf(
                    "id" to navEnhet.id,
                    "nav_enhet_nummer" to navEnhet.enhetsnummer,
                    "navn" to navEnhet.navn,
                    "modified_at" to LocalDateTime.now(),
                ),
            ).map(::rowMapper).asSingle

            it.run(query)
        } ?: throw RuntimeException("Noe gikk galt ved lagring av nav-enhet")
    }

    fun get(enhetsnummer: String): NavEnhetDbo? = Database.query {
        val query = queryOf(
            """select * from nav_enhet where nav_enhet_nummer = :nav_enhet_nummer""",
            mapOf("nav_enhet_nummer" to enhetsnummer),
        ).map(::rowMapper).asSingle

        it.run(query)
    }

    fun get(id: UUID): NavEnhetDbo? = Database.query {
        val query = queryOf(
            """select * from nav_enhet where id = :id""",
            mapOf("id" to id),
        ).map(::rowMapper).asSingle

        it.run(query)
    }

    fun getMany(enhetIder: List<UUID>) = Database.query {
        if (enhetIder.isEmpty()) return@query emptyList()

        val statement = "select * from nav_enhet where id in (${enhetIder.joinToString { "?" }})"

        val query = queryOf(
            statement,
            *enhetIder.toTypedArray(),
        )

        it.run(query.map(::rowMapper).asList)
    }
}
