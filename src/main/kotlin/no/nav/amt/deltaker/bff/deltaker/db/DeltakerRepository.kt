package no.nav.amt.deltaker.bff.deltaker.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.deltaker.bff.db.toPGObject
import no.nav.amt.deltaker.bff.deltaker.model.AVSLUTTENDE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerIdOgStatus
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.deltaker.internalapis.paamelding.response.OpprettKladdResponse
import no.nav.amt.lib.models.person.NavBruker
import no.nav.amt.lib.models.person.address.Adressebeskyttelse
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeltakerRepository {
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
            adresse = row.stringOrNull("nb.adresse")?.let { objectMapper.readValue(it) },
            erSkjermet = row.boolean("nb.er_skjermet"),
            navEnhetId = row.uuidOrNull("nb.nav_enhet_id"),
            navVeilederId = row.uuidOrNull("nb.nav_veileder_id"),
            telefon = null,
            epost = null,
        ),
        deltakerliste = DeltakerlisteRepository.rowMapper(row),
        startdato = row.localDateOrNull("d.startdato"),
        sluttdato = row.localDateOrNull("d.sluttdato"),
        dagerPerUke = row.floatOrNull("d.dager_per_uke"),
        deltakelsesprosent = row.floatOrNull("d.deltakelsesprosent"),
        bakgrunnsinformasjon = row.stringOrNull("d.bakgrunnsinformasjon"),
        deltakelsesinnhold = row.stringOrNull("d.innhold")?.let { objectMapper.readValue(it) },
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
        erManueltDeltMedArrangor = row.boolean("d.er_manuelt_delt_med_arrangor"),
    )

    fun upsert(deltaker: Deltaker) = Database.query { session ->
        val sql =
            """
            insert into deltaker(
                id, person_id, deltakerliste_id, startdato, sluttdato, dager_per_uke, 
                deltakelsesprosent, bakgrunnsinformasjon, innhold, historikk, kan_endres, modified_at,
                er_manuelt_delt_med_arrangor
            )
            values (
                :id, :person_id, :deltakerlisteId, :startdato, :sluttdato, :dagerPerUke, 
                :deltakelsesprosent, :bakgrunnsinformasjon, :innhold, :historikk, :kan_endres, :modified_at,
                :er_manuelt_delt_med_arrangor
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
                modified_at          = :modified_at,
                er_manuelt_delt_med_arrangor = :er_manuelt_delt_med_arrangor
                
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
            "innhold" to toPGObject(deltaker.deltakelsesinnhold),
            "historikk" to toPGObject(deltaker.historikk),
            "kan_endres" to deltaker.kanEndres,
            "modified_at" to deltaker.sistEndret,
            "er_manuelt_delt_med_arrangor" to deltaker.erManueltDeltMedArrangor,
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

    fun getMany(ider: List<UUID>) = Database.query {
        if (ider.isEmpty()) return@query emptyList()

        val sql = getDeltakerSql("where d.id = any(:ider) and ds.gyldig_til is null")

        val query = queryOf(sql, mapOf("ider" to ider.toTypedArray())).map(::rowMapper).asList

        it.run(query)
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

    fun getKladdForDeltakerliste(deltakerlisteId: UUID, personident: String) = Database.query {
        val sql = getDeltakerSql(
            """ where d.deltakerliste_id = :deltakerliste_id
                    and nb.personident = :personident
                    and ds.type = 'KLADD'
                    and ds.gyldig_til is null
            """.trimMargin(),
        )

        val query = queryOf(
            sql,
            mapOf(
                "deltakerliste_id" to deltakerlisteId,
                "personident" to personident,
            ),
        ).map(::rowMapper).asList
        it.run(query).firstOrNull()
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
              and d.kan_endres = true
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

    fun settKanEndres(ider: List<UUID>, kanEndres: Boolean) = Database.query {
        if (ider.isEmpty()) {
            return@query
        }
        val sql =
            """
            update deltaker
            set kan_endres = :kan_endres
            where id =any (:ider);
            """.trimIndent()
        val parameters = mapOf(
            "kan_endres" to kanEndres,
            "ider" to ider.toTypedArray(),
        )
        it.update(
            queryOf(sql, parameters),
        )
    }

    fun getDeltakerIdOgStatusForDeltakelser(personident: String, deltakerlisteId: UUID) = Database.query { session ->
        val sql =
            """
            SELECT d.id           AS "d.id",
                   d.kan_endres   AS "d.kan_endres",
                   ds.id          AS "ds.id",
                   ds.deltaker_id AS "ds.deltaker_id",
                   ds.type        AS "ds.type",
                   ds.aarsak      AS "ds.aarsak",
                   ds.gyldig_fra  AS "ds.gyldig_fra",
                   ds.gyldig_til  AS "ds.gyldig_til",
                   ds.created_at  AS "ds.created_at",
                   ds.modified_at AS "ds.modified_at"
            FROM deltaker d
                     JOIN nav_bruker nb ON d.person_id = nb.person_id
                     JOIN deltaker_status ds ON d.id = ds.deltaker_id
                     JOIN deltakerliste dl ON d.deltakerliste_id = dl.id
            WHERE nb.personident = :personident
              AND d.deltakerliste_id = :deltakerliste_id
              AND ds.gyldig_til IS NULL;
            """.trimMargin()

        val query = queryOf(
            sql,
            mapOf(
                "personident" to personident,
                "deltakerliste_id" to deltakerlisteId,
            ),
        ).map { row ->
            DeltakerIdOgStatus(
                id = row.uuid("d.id"),
                status = DeltakerStatus(
                    id = row.uuid("ds.id"),
                    type = row.string("ds.type").let { DeltakerStatus.Type.valueOf(it) },
                    aarsak = row.stringOrNull("ds.aarsak")?.let { objectMapper.readValue(it) },
                    gyldigFra = row.localDateTime("ds.gyldig_fra"),
                    gyldigTil = row.localDateTimeOrNull("ds.gyldig_til"),
                    opprettet = row.localDateTime("ds.created_at"),
                ),
                kanEndres = row.boolean("d.kan_endres"),
            )
        }.asList
        session.run(query)
    }

    fun create(kladd: OpprettKladdResponse) = Database.query {
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
            "innhold" to toPGObject(kladd.deltakelsesinnhold),
        )

        it.transaction { tx ->
            tx.update(queryOf(sql, parameters))
            tx.update(insertStatusQuery(kladd.status, kladd.id))
            tx.update(deaktiverTidligereStatuserQuery(kladd.status, kladd.id))
        }
    }

    fun update(deltaker: Deltakeroppdatering, isSynchronousInvocation: Boolean = true) = Database.query { session ->
        val eksisterendeDeltaker = get(deltaker.id).getOrNull()

        val skalOppdatereStatus: Boolean = isSynchronousInvocation ||
            (
                eksisterendeDeltaker?.let {
                    deltaker.status.opprettet.truncatedTo(ChronoUnit.MILLIS) >= it.status.opprettet.truncatedTo(ChronoUnit.MILLIS)
                } ?: true
            )

        session.transaction { tx ->
            tx.update(queryOf(updateDeltakerSQL(), updateDeltakerParams(deltaker)))
            if (skalOppdatereStatus) {
                tx.update(insertStatusQuery(deltaker.status, deltaker.id))
                tx.update(deaktiverTidligereStatuserQuery(deltaker.status, deltaker.id))
            }
        }
    }

    fun updateBatch(deltakere: List<Deltakeroppdatering>) = Database.query { session ->
        val deltakerParams = deltakere.map { batchUpdateDeltakerParams(it) }
        val statusParams = deltakere.map { insertStatusParams(it.status, it.id) }
        val deaktiverTidligereStatuserParams = deltakere.map { deaktiverTidligereStatuserParams(it.status, it.id) }

        val sql = updateDeltakerSQL(true)

        session.transaction { tx ->
            tx.batchPreparedNamedStatement(sql, deltakerParams)
            tx.batchPreparedNamedStatement(insertStatusSQL, statusParams)
            tx.batchPreparedNamedStatement(deaktiverTidligereStatuserSQL, deaktiverTidligereStatuserParams)
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

    fun getDeltakereMedFlereGyldigeStatuser() = Database.query { session ->
        val sql =
            """
            WITH statuser as (SELECT deltaker_id, COUNT(*) AS c
                  FROM deltaker_status
                  WHERE deltaker_status.gyldig_til IS NULL
                  GROUP BY deltaker_id)
            
            SELECT * from statuser WHERE c > 1;
            """.trimMargin()

        val query = queryOf(
            sql,
            emptyMap(),
        ).map {
            it.uuid("deltaker_id")
        }.asList
        session.run(query)
    }

    fun getForDeltakerliste(deltakerlisteId: UUID) = Database.query { session ->
        val params = mapOf("deltakerliste_id" to deltakerlisteId)
        session.run(
            queryOf(
                getDeltakerSql("where dl.id = :deltakerliste_id and ds.gyldig_til is null"),
                params,
            ).map(::rowMapper).asList,
        )
    }

    fun deaktiverUkritiskTidligereStatuserQuery(status: DeltakerStatus, deltakerId: UUID) = Database.query { session ->
        val query = queryOf(deaktiverTidligereStatuserSQL, deaktiverTidligereStatuserParams(status, deltakerId))
        session.update(query)
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
                   d.er_manuelt_delt_med_arrangor as "d.er_manuelt_delt_med_arrangor",
                   nb.personident as "nb.personident",
                   nb.fornavn as "nb.fornavn",
                   nb.mellomnavn as "nb.mellomnavn",
                   nb.etternavn as "nb.etternavn",
                   nb.adressebeskyttelse as "nb.adressebeskyttelse",
                   nb.oppfolgingsperioder as "nb.oppfolgingsperioder",
                   nb.innsatsgruppe as "nb.innsatsgruppe",
                   nb.er_skjermet as "nb.er_skjermet",
                   nb.adresse as "nb.adresse",
                   nb.nav_enhet_id as "nb.nav_enhet_id",
                   nb.nav_veileder_id as "nb.nav_veileder_id",
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
                   dl.apent_for_pamelding as "dl.apent_for_pamelding",
                   dl.antall_plasser as "dl.antall_plasser",
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

    private fun batchUpdateDeltakerParams(deltaker: Deltakeroppdatering) = mapOf(
        "id" to deltaker.id,
        "startdato" to deltaker.startdato,
        "sluttdato" to deltaker.sluttdato,
        "dagerPerUke" to deltaker.dagerPerUke,
        "deltakelsesprosent" to deltaker.deltakelsesprosent,
        "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
        "innhold" to toPGObject(deltaker.deltakelsesinnhold),
        "historikk" to toPGObject(deltaker.historikk),
        "modified_at" to deltaker.sistEndret,
        "er_manuelt_delt_med_arrangor" to deltaker.erManueltDeltMedArrangor,
    )

    private fun updateDeltakerParams(deltaker: Deltakeroppdatering) = mapOf(
        "id" to deltaker.id,
        "startdato" to deltaker.startdato,
        "sluttdato" to deltaker.sluttdato,
        "dagerPerUke" to deltaker.dagerPerUke,
        "deltakelsesprosent" to deltaker.deltakelsesprosent,
        "bakgrunnsinformasjon" to deltaker.bakgrunnsinformasjon,
        "innhold" to toPGObject(deltaker.deltakelsesinnhold),
        "historikk" to toPGObject(deltaker.historikk),
        "modified_at" to deltaker.sistEndret,
    )

    private fun updateDeltakerSQL(erBatchUpdate: Boolean = false): String =
        """
        update deltaker set 
            startdato            = :startdato,
            sluttdato            = :sluttdato,
            dager_per_uke        = :dagerPerUke,
            deltakelsesprosent   = :deltakelsesprosent,
            bakgrunnsinformasjon = :bakgrunnsinformasjon,
            innhold              = :innhold,
            historikk            = :historikk,
            ${if (erBatchUpdate) "er_manuelt_delt_med_arrangor = :er_manuelt_delt_med_arrangor," else ""}
            modified_at          = :modified_at
            where id = :id
        """.trimIndent()

    private fun insertStatusQuery(status: DeltakerStatus, deltakerId: UUID): Query =
        queryOf(insertStatusSQL, insertStatusParams(status, deltakerId))

    private val insertStatusSQL =
        """
        insert into deltaker_status(id, deltaker_id, type, aarsak, gyldig_fra, created_at)
        values (:id, :deltaker_id, :type, :aarsak, :gyldig_fra, :created_at)
        on conflict (id) do nothing
        ;
        """.trimIndent()

    private fun insertStatusParams(status: DeltakerStatus, deltakerId: UUID) = mapOf(
        "id" to status.id,
        "deltaker_id" to deltakerId,
        "type" to status.type.name,
        "aarsak" to toPGObject(status.aarsak),
        "gyldig_fra" to status.gyldigFra,
        "created_at" to status.opprettet,
    )

    private fun deaktiverTidligereStatuserQuery(status: DeltakerStatus, deltakerId: UUID): Query =
        queryOf(deaktiverTidligereStatuserSQL, deaktiverTidligereStatuserParams(status, deltakerId))

    private val deaktiverTidligereStatuserSQL =
        """
        update deltaker_status
        set gyldig_til = current_timestamp
        where deltaker_id = :deltaker_id 
          and id != :id 
          and gyldig_til is null;
        """.trimIndent()

    private fun deaktiverTidligereStatuserParams(status: DeltakerStatus, deltakerId: UUID) =
        mapOf("id" to status.id, "deltaker_id" to deltakerId)
}
