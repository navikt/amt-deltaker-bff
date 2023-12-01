package no.nav.amt.deltaker.bff.navansatt

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.Database
import java.time.LocalDateTime
import java.util.UUID

class NavAnsattRepository {
    private fun rowMapper(row: Row) = NavAnsatt(
        id = row.uuid("id"),
        navident = row.string("nav_ident"),
        navn = row.string("navn"),
    )

    fun upsert(navAnsatt: NavAnsatt) {
        val sql = """
            INSERT INTO nav_ansatt(id, nav_ident, navn, modified_at)
            VALUES (:id, :nav_ident, :navn, :modified_at) 
            ON CONFLICT (id) DO UPDATE SET
                nav_ident = :nav_ident,
                navn = :navn,
                modified_at = :modified_at
        """.trimIndent()

        Database.query {
            val query = queryOf(
                sql,
                mapOf(
                    "id" to navAnsatt.id,
                    "nav_ident" to navAnsatt.navident,
                    "navn" to navAnsatt.navn,
                    "modified_at" to LocalDateTime.now(),
                ),
            )
            it.update(query)
        }
    }

    fun get(id: UUID): NavAnsatt? {
        return Database.query {
            val query = queryOf(
                """select * from nav_ansatt where id = :id""",
                mapOf("id" to id),
            ).map(::rowMapper).asSingle

            it.run(query)
        }
    }

    fun get(navIdent: String): NavAnsatt? {
        return Database.query {
            val query = queryOf(
                """select * from nav_ansatt where nav_ident = :nav_ident""",
                mapOf("nav_ident" to navIdent),
            ).map(::rowMapper).asSingle

            it.run(query)
        }
    }

    fun delete(id: UUID) = Database.query {
        val query = queryOf(
            """delete from nav_ansatt where id = :id""",
            mapOf("id" to id),
        )
        it.update(query)
    }
}
