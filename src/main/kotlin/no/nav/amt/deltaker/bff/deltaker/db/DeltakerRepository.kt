package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.AVSLUTTENDE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Innsatsgruppe
import no.nav.amt.deltaker.bff.deltaker.navbruker.Adressebeskyttelse
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.lib.utils.database.Database
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeltakerRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun rowMapper(row: Row) = Deltaker(
        id = row.uuid("d.id"),
        navBruker = NavBruker(
            personId = row.uuid("d.person_id"),
            personident = row.string("nb.personident"),
            fornavn = row.string("nb.fornavn"),
            mellomnavn = row.stringOrNull("nb.mellomnavn"),
            etternavn = row.string("nb.etternavn"),
            adressebeskyttelse = row.stringOrNull("nb.adressebeskyttelse")?.let { Adressebeskyttelse.valueOf(it) },
            oppfolgingsperioder = row.stringOrNull("nb.oppfolgingsperioder")?.let { objectMapper.readValue(it) } ?: emptyList(),
            innsatsgruppe = row.stringOrNull("nb.innsatsgruppe")?.let { Innsatsgruppe.valueOf(it) },
        ),
        deltakerliste = DeltakerlisteRepository.rowMapper(row),
        startdato = row.localDateOrNull("d.startdato"),
        sluttdato = row.localDateOrNull("d.sluttdato"),
        dagerPerUke = row.floatOrNull("d.dager_per_uke"),
        deltakelsesprosent = row.floatOrNull("d.deltakelsesprosent"),
        bakgrunnsinformasjon = row.stringOrNull("d.bakgrunnsinformasjon"),
        innhold = objectMapper.readValue(row.string("d.innhold")),
        status = DeltakerStatus(
            id = row.uuid("ds.id"),
            type = row.string("ds.type").let { DeltakerStatus.Type.valueOf(it) },
            aarsak = row.stringOrNull("ds.aarsak")?.let { objectMapper.readValue(it) },
            gyldigFra = row.localDateTime("ds.gyldig_fra"),
            gyldigTil = row.localDateTimeOrNull("ds.gyldig_til"),
            opprettet = row.localDateTime("ds.created_at"),
        ),
        historikk = row.string("d.historikk").let { list ->
            objectMapper.readValue<List<DeltakerHistorikk>>(list.trim())
        },
        kanEndres = row.boolean("d.kan_endres"),
        sistEndret = row.localDateTime("d.modified_at"),
    )

    fun upsert(deltaker: Deltaker) = Database.query { session ->
        val sql =
            """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold, historikk, kan_endres
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold, :historikk, :kan_endres
            )
            on conflict (id) do update set 
                person_id          = :person_id,
                startdato            = :startdato,
                sluttdato            = :sluttdato,
                dager_per_uke        = :dagerPerUke,
                deltakelsesprosent   = :deltakelsesprosent,
                bakgrunnsinformasjon = :bakgrunnsinformasjon,
                innhold              = :innhold,
                historikk            = :historikk,
                kan_endres           = :kan_endres,
                modified_at          = current_timestamp
            """.trimIndent()

        val parameters = mapOf(
            "id" to deltaker.id,
            "person_id" to deltaker.navBruker.personId,
            "deltakerlisteId" to deltaker.deltakerliste.id,
            "startdato" to deltaker.startdato,
            "sluttdato" to deltaker.sluttdato,
            "dagerPerUke" to deltaker.dagerPerUke,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
            "innhold" to toPGObject(deltaker.innhold),
            "historikk" to toPGObject(deltaker.historikk),
            "kan_endres" to deltaker.kanEndres,
        )

        session.transaction { tx ->
            tx.update(queryOf(sql, parameters))
            tx.update(insertStatusQuery(deltaker.status, deltaker.id))
            tx.update(deaktiverTidligereStatuserQuery(deltaker.status, deltaker.id))
        }
    }

    fun get(id: UUID) = Database.query {
        val sql = getDeltakerSql("where d.id = :id and ds.gyldig_til is null")

        val query = queryOf(sql, mapOf("id" to id)).map(::rowMapper).asSingle
        it.run(query)?.let { d -> Result.success(d) }
            ?: Result.failure(NoSuchElementException("Ingen deltaker med id $id"))
    }

    fun getMany(personident: String, deltakerlisteId: UUID) = Database.query {
        val sql = getDeltakerSql(
            """ where nb.personident = :personident 
                    and d.deltakerliste_id = :deltakerliste_id 
                    and ds.gyldig_til is null
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "personident" to personident,
                "deltakerliste_id" to deltakerlisteId,
            ),
        ).map(::rowMapper).asList
        it.run(query)
    }

    fun getMany(personident: String) = Database.query {
        val sql = getDeltakerSql(
            """ where nb.personident = :personident
                    and ds.gyldig_til is null
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "personident" to personident,
            ),
        ).map(::rowMapper).asList
        it.run(query)
    }

    fun getKladderForDeltakerliste(deltakerlisteId: UUID) = Database.query {
        val sql = getDeltakerSql(
            """ where d.deltakerliste_id = :deltakerliste_id 
                    and ds.type = 'KLADD'
                    and ds.gyldig_til is null
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "deltakerliste_id" to deltakerlisteId,
            ),
        ).map(::rowMapper).asList
        it.run(query)
    }

    fun getTidligereAvsluttedeDeltakelser(deltakerId: UUID) = Database.query { session ->
        val avsluttendeDeltakerStatuser = AVSLUTTENDE_STATUSER.map { it.name }
        val sql =
            """
            select d2.id
            from deltaker d
                     join deltaker d2 on d.person_id = d2.person_id
                and d.deltakerliste_id = d2.deltakerliste_id
                     inner join deltaker_status ds on d2.id = ds.deltaker_id
            where d.id = ?
              and ds.gyldig_til is null
            and ds.type in (${avsluttendeDeltakerStatuser.joinToString { "?" }})
            and d2.id != d.id;
            """.trimMargin()

        val query = queryOf(
            sql,
            deltakerId,
            *avsluttendeDeltakerStatuser.toTypedArray(),
        ).map {
            it.uuid("id")
        }.asList
        session.run(query)
    }

    fun getUtdaterteKladder(sistEndret: LocalDateTime) = Database.query { session ->
        val sql = getDeltakerSql(
            """
            where ds.gyldig_til is null
                and ds.type = 'KLADD'
                and d.modified_at < :sist_endret
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "sist_endret" to sistEndret,
            ),
        ).map(::rowMapper).asList
        session.run(query)
    }

    fun getDeltakerStatuser(deltakerId: UUID) = Database.query { session ->
        val sql =
            """
            select * from deltaker_status where deltaker_id = :deltaker_id
            """.trimIndent()

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId))
            .map {
                DeltakerStatus(
                    id = it.uuid("id"),
                    type = it.string("type").let { t -> DeltakerStatus.Type.valueOf(t) },
                    aarsak = it.stringOrNull("aarsak")?.let { aarsak -> objectMapper.readValue(aarsak) },
                    gyldigFra = it.localDateTime("gyldig_fra"),
                    gyldigTil = it.localDateTimeOrNull("gyldig_til"),
                    opprettet = it.localDateTime("created_at"),
                )
            }.asList

        session.run(query)
    }

    fun delete(deltakerId: UUID) = Database.query { session ->
        session.transaction { tx ->
            tx.update(slettStatus(deltakerId))
            tx.update(slettDeltaker(deltakerId))
        }
    }

    fun settKanIkkeEndres(ider: List<UUID>) = Database.query {
        if (ider.isEmpty()) {
            return@query
        }
        val sql =
            """
            update deltaker
            set kan_endres = false
            where id in (${ider.joinToString { "?" }});
            """.trimIndent()

        it.update(
            queryOf(sql, *ider.toTypedArray()),
        )
    }

    fun create(kladd: KladdResponse) = Database.query {
        val sql =
            """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold
            )
            """.trimIndent()

        val parameters = mapOf(
            "id" to kladd.id,
            "person_id" to kladd.navBruker.personId,
            "deltakerlisteId" to kladd.deltakerlisteId,
            "startdato" to kladd.startdato,
            "sluttdato" to kladd.sluttdato,
            "dagerPerUke" to kladd.dagerPerUke,
            "deltakelsesprosent" to kladd.deltakelsesprosent,
            "bakgrunnsinformasjon" to kladd.bakgrunnsinformasjon,
            "innhold" to toPGObject(kladd.innhold),
        )

        it.transaction { tx ->
            tx.update(queryOf(sql, parameters))
            tx.update(insertStatusQuery(kladd.status, kladd.id))
            tx.update(deaktiverTidligereStatuserQuery(kladd.status, kladd.id))
        }
    }

    fun update(deltaker: Deltakeroppdatering) = Database.query { session ->
        if (!skalOppdateres(deltaker)) {
            return@query
        }

        val sql =
            """
            update deltaker set 
                startdato            = :startdato,
                sluttdato            = :sluttdato,
                dager_per_uke        = :dagerPerUke,
                deltakelsesprosent   = :deltakelsesprosent,
                bakgrunnsinformasjon = :bakgrunnsinformasjon,
                innhold              = :innhold,
                historikk            = :historikk,
                modified_at          = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to deltaker.id,
            "startdato" to deltaker.startdato,
            "sluttdato" to deltaker.sluttdato,
            "dagerPerUke" to deltaker.dagerPerUke,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
            "innhold" to toPGObject(deltaker.innhold),
            "historikk" to toPGObject(deltaker.historikk),
        )

        session.transaction { tx ->
            tx.update(insertStatusQuery(deltaker.status, deltaker.id))
            tx.update(deaktiverTidligereStatuserQuery(deltaker.status, deltaker.id))
            tx.update(queryOf(sql, params))
        }
    }

    private fun slettStatus(deltakerId: UUID): Query {
        val sql =
            """
            delete from deltaker_status
            where deltaker_id = :deltaker_id;
            """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
        )

        return queryOf(sql, params)
    }

    private fun slettDeltaker(deltakerId: UUID): Query {
        val sql =
            """
            delete from deltaker
            where id = :deltaker_id;
            """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
        )

        return queryOf(sql, params)
    }

    private fun insertStatusQuery(status: DeltakerStatus, deltakerId: UUID): Query {
        val sql =
            """
            insert into deltaker_status(id, deltaker_id, type, aarsak, gyldig_fra, created_at) 
            values (:id, :deltaker_id, :type, :aarsak, :gyldig_fra, :opprettet) 
            on conflict (id) do nothing;
            """.trimIndent()

        val params = mapOf(
            "id" to status.id,
            "deltaker_id" to deltakerId,
            "type" to status.type.name,
            "aarsak" to toPGObject(status.aarsak),
            "gyldig_fra" to status.gyldigFra,
            "opprettet" to status.opprettet,
        )

        return queryOf(sql, params)
    }

    private fun deaktiverTidligereStatuserQuery(status: DeltakerStatus, deltakerId: UUID): Query {
        val sql =
            """
            update deltaker_status
            set gyldig_til = current_timestamp
            where deltaker_id = :deltaker_id 
              and id != :id 
              and gyldig_fra < :ny_gyldig_fra
              and gyldig_til is null;
            """.trimIndent()

        return queryOf(sql, mapOf("id" to status.id, "deltaker_id" to deltakerId, "ny_gyldig_fra" to status.gyldigFra))
    }

    private fun getDeltakerSql(where: String = "") = """
            select d.id as "d.id",
                   d.person_id as "d.person_id",
                   d.deltakerliste_id as "d.deltakerliste_id",
                   d.startdato as "d.startdato",
                   d.sluttdato as "d.sluttdato",
                   d.dager_per_uke as "d.dager_per_uke",
                   d.deltakelsesprosent as "d.deltakelsesprosent",
                   d.bakgrunnsinformasjon as "d.bakgrunnsinformasjon",
                   d.innhold as "d.innhold",
                   d.historikk as "d.historikk",
                   d.modified_at as "d.modified_at",
                   d.kan_endres as "d.kan_endres",
                   nb.personident as "nb.personident",
                   nb.fornavn as "nb.fornavn",
                   nb.mellomnavn as "nb.mellomnavn",
                   nb.etternavn as "nb.etternavn",
                   nb.adressebeskyttelse as "nb.adressebeskyttelse",
                   nb.oppfolgingsperioder as "nb.oppfolgingsperioder",
                   nb.innsatsgruppe as "nb.innsatsgruppe",
                   ds.id as "ds.id",
                   ds.deltaker_id as "ds.deltaker_id",
                   ds.type as "ds.type",
                   ds.aarsak as "ds.aarsak",
                   ds.gyldig_fra as "ds.gyldig_fra",
                   ds.gyldig_til as "ds.gyldig_til",
                   ds.created_at as "ds.created_at",
                   ds.modified_at as "ds.modified_at",
                   dl.id as "dl.id",
                   dl.navn as "dl.navn",
                   dl.status as "dl.status",
                   dl.start_dato as "dl.start_dato",
                   dl.slutt_dato as "dl.slutt_dato",
                   dl.oppstart as "dl.oppstart",
                   a.id as "a.id",
                   a.navn as "a.navn",
                   a.organisasjonsnummer as "a.organisasjonsnummer",
                   a.overordnet_arrangor_id as "a.overordnet_arrangor_id",
                   oa.navn as "oa.navn",
                   t.id as "t.id",
                   t.navn as "t.navn",
                   t.tiltakskode as "t.tiltakskode",
                   t.type as "t.type",
                   t.innsatsgrupper as "t.innsatsgrupper",
                   t.innhold as "t.innhold"
            from deltaker d 
                join nav_bruker nb on d.person_id = nb.person_id
                join deltaker_status ds on d.id = ds.deltaker_id
                join deltakerliste dl on d.deltakerliste_id = dl.id
                join arrangor a on a.id = dl.arrangor_id
                join tiltakstype t on t.id = dl.tiltakstype_id
                left join arrangor oa on oa.id = a.overordnet_arrangor_id
                $where
      """

    private fun skalOppdateres(oppdatering: Deltakeroppdatering): Boolean {
        val eksisterendeDeltaker = get(oppdatering.id).getOrThrow()

        if (oppdatering.forcedUpdate == true) {
            log.info("Tvungen oppdtaering på deltaker med id ${oppdatering.id}")
            return true
        }

        if (eksisterendeDeltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT) {
            log.warn("Har mottatt oppdatering på feilregistrert deltaker, ignorerer, ${oppdatering.id}")
            return false
        }

        val erUtkast = oppdatering.status.type == DeltakerStatus.Type.UTKAST_TIL_PAMELDING &&
            eksisterendeDeltaker.status.type == DeltakerStatus.Type.UTKAST_TIL_PAMELDING

        val oppdateringHarNyereStatus = oppdatering.status.opprettet.truncatedTo(ChronoUnit.MILLIS) >
            eksisterendeDeltaker.status.opprettet.truncatedTo(ChronoUnit.MILLIS)

        val kanOppdateres = eksisterendeDeltaker.kanEndres &&
            (
                oppdatering.historikk.size > eksisterendeDeltaker.historikk.size ||
                    oppdateringHarNyereStatus ||
                    erUtkast
            )

        if (!kanOppdateres) {
            log.info(
                """
                Deltaker ${oppdatering.id} skal ikke oppdateres
                oppdatering.historikk:        ${oppdatering.historikk.size}
                deltaker.historikk:           ${eksisterendeDeltaker.historikk.size}
                oppdatering.status.opprettet: ${oppdatering.status.opprettet} 
                deltaker.status.opprettet:    ${eksisterendeDeltaker.status.opprettet}
                deltaker.kan_endres:          ${eksisterendeDeltaker.kanEndres}
                """.trimIndent(),
            )
        }

        return kanOppdateres
    }

    fun oppdaterSistBesokt(id: UUID, sistBesokt: ZonedDateTime) = Database.query {
        val sql =
            """
            update deltaker
            set sist_besokt = :sist_besokt
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to id,
            "sist_besokt" to sistBesokt,
        )

        it.update(queryOf(sql, params))
    }
}
