package no.nav.amt.deltaker.bff.deltaker.navbruker

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.Database
import java.util.UUID

class NavBrukerRepository {

    private fun rowMapper(row: Row) = NavBruker(
        personId = row.uuid("personId"),
        personident = row.string("personident"),
        fornavn = row.string("fornavn"),
        mellomnavn = row.stringOrNull("mellomnavn"),
        etternavn = row.string("etternavn"),
    )

    fun upsert(bruker: NavBruker) = Database.query {
        val sql = """
            insert into nav_bruker(personId, personident, fornavn, mellomnavn, etternavn) 
            values (:personId, :personident, :fornavn, :mellomnavn, :etternavn)
            on conflict (personId) do update set
                personident = :personident,
                fornavn = :fornavn,
                mellomnavn = :mellomnavn,
                etternavn = :etternavn
            returning *
        """.trimIndent()

        val params = mapOf(
            "personId" to bruker.personId,
            "personident" to bruker.personident,
            "fornavn" to bruker.fornavn,
            "mellomnavn" to bruker.mellomnavn,
            "etternavn" to bruker.etternavn,
        )

        it.run(queryOf(sql, params).map(::rowMapper).asSingle)
            ?.let { b -> Result.success(b) }
            ?: Result.failure(NoSuchElementException("Noe gikk galt med upsert av bruker ${bruker.personId}"))
    }

    fun get(personId: UUID) = Database.query {
        val query = queryOf(
            statement = "select * from nav_bruker where personId = :personId",
            paramMap = mapOf("personId" to personId),
        )

        it.run(query.map(::rowMapper).asSingle)
            ?.let { b -> Result.success(b) }
            ?: Result.failure(NoSuchElementException("Fant ikke bruker $personId"))
    }
}
