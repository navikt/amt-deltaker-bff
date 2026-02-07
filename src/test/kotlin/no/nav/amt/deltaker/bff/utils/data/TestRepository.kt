package no.nav.amt.deltaker.bff.utils.data

import kotliquery.queryOf
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerStatusRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetDbo
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavAnsatt
import no.nav.amt.lib.models.deltaker.Arrangor
import no.nav.amt.lib.models.person.NavBruker
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

object TestRepository {
    fun cleanDatabase() = Database.query { session ->
        val tables = listOf(
            "tiltakskoordinator_deltakerliste_tilgang",
            "ulest_hendelse",
            "forslag",
            "vurdering",
            "deltaker_status",
            "deltaker",
            "nav_bruker",
            "nav_ansatt",
            "nav_enhet",
            "deltakerliste",
            "arrangor",
        )
        tables.forEach {
            val query = queryOf(
                """delete from $it""",
                emptyMap(),
            )

            session.update(query)
        }
    }

    fun insert(deltakerliste: Deltakerliste, overordnetArrangor: Arrangor? = null) {
        TiltakstypeRepository().upsert(deltakerliste.tiltak)

        if (overordnetArrangor != null) {
            ArrangorRepository().upsert(overordnetArrangor)
        }
        ArrangorRepository().upsert(deltakerliste.arrangor.arrangor)

        DeltakerlisteRepository().upsert(deltakerliste)
    }

    fun insert(deltaker: Deltaker) {
        insert(deltaker.navBruker)
        insert(deltaker.deltakerliste)
        DeltakerRepository().upsert(deltaker)
        DeltakerStatusRepository.insertIfNotExists(deltaker.id, deltaker.status)
    }

    fun insert(navEnhetDbo: NavEnhetDbo) {
        val sql =
            """
            INSERT INTO nav_enhet (
                id, 
                nav_enhet_nummer, 
                navn, 
                modified_at
            )
            VALUES (
                :id, 
                :nav_enhet_nummer, 
                :navn, 
                :modified_at
            ) 
            ON CONFLICT (id) DO NOTHING
            """.trimIndent()

        val params = mapOf(
            "id" to navEnhetDbo.id,
            "nav_enhet_nummer" to navEnhetDbo.enhetsnummer,
            "navn" to navEnhetDbo.navn,
            "modified_at" to navEnhetDbo.sistEndret,
        )

        Database.query { session -> session.update(queryOf(sql, params)) }
    }

    fun insert(bruker: NavBruker) {
        val navVeilederId: UUID? = bruker.navVeilederId
        if (navVeilederId != null) {
            NavAnsattRepository().upsert(lagNavAnsatt(navVeilederId))
        }

        val navEnhetId: UUID? = bruker.navEnhetId
        if (navEnhetId != null) {
            NavEnhetRepository().upsert(TestData.lagNavEnhet(navEnhetId))
        }

        NavBrukerRepository().upsert(bruker)
    }

    fun getDeltakerSistBesokt(deltakerId: UUID) = Database.query {
        val sql =
            """
            select sist_besokt from deltaker where id = ?
            """.trimIndent()

        it.run(queryOf(sql, deltakerId).map { row -> row.zonedDateTime("sist_besokt") }.asSingle)
    }
}
