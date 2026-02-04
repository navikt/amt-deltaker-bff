package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.apiclients.DtoMappers.toDeltakeroppdatering
import no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.apiclients.paamelding.PaameldingClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerStatusRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagRepository
import no.nav.amt.deltaker.bff.deltaker.model.AKTIVE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.utils.database.Database
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val paameldingClient: PaameldingClient,
    private val navEnhetService: NavEnhetService,
    private val forslagRepository: ForslagRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun oppdaterDeltaker(
        deltaker: Deltaker,
        endring: DeltakerEndring.Endring,
        endretAv: String,
        endretAvEnhet: String,
        forslagId: UUID? = null,
    ): Deltaker {
        navEnhetService.hentOpprettEllerOppdaterNavEnhet(endretAvEnhet)
        val deltakeroppdatering = when (endring) {
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> {
                amtDeltakerClient
                    .endreBakgrunnsinformasjon(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        endring = endring,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreInnhold -> {
                amtDeltakerClient
                    .endreInnhold(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        innhold = Deltakelsesinnhold(
                            ledetekst = endring.ledetekst,
                            innhold = endring.innhold,
                        ),
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.AvsluttDeltakelse -> {
                amtDeltakerClient
                    .avsluttDeltakelse(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        sluttdato = endring.sluttdato,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreAvslutning -> {
                amtDeltakerClient
                    .endreAvslutning(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        harFullfort = endring.harFullfort,
                        sluttdato = endring.sluttdato,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.AvbrytDeltakelse -> {
                amtDeltakerClient
                    .avbrytDeltakelse(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        sluttdato = endring.sluttdato,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> {
                amtDeltakerClient
                    .endreDeltakelsesmengde(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        deltakelsesprosent = endring.deltakelsesprosent,
                        dagerPerUke = endring.dagerPerUke,
                        begrunnelse = endring.begrunnelse,
                        gyldigFra = endring.gyldigFra,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreSluttarsak -> {
                amtDeltakerClient
                    .endreSluttaarsak(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreSluttdato -> {
                amtDeltakerClient
                    .endreSluttdato(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        sluttdato = endring.sluttdato,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreStartdato -> {
                amtDeltakerClient
                    .endreStartdato(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        startdato = endring.startdato,
                        sluttdato = endring.sluttdato,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.ForlengDeltakelse -> {
                amtDeltakerClient
                    .forlengDeltakelse(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        sluttdato = endring.sluttdato,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.IkkeAktuell -> {
                amtDeltakerClient
                    .ikkeAktuell(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.ReaktiverDeltakelse -> {
                val response = amtDeltakerClient.reaktiverDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    begrunnelse = endring.begrunnelse,
                )
                slettKladd(deltaker.deltakerliste.id, deltaker.navBruker.personident)
                response.toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.FjernOppstartsdato -> {
                amtDeltakerClient
                    .fjernOppstartsdato(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        begrunnelse = endring.begrunnelse,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }
        }
        oppdaterDeltaker(deltakeroppdatering)
        return deltaker.oppdater(deltakeroppdatering)
    }

    // benyttes av DeltakerV2Consumer
    fun oppdaterDeltakerLaas(
        deltakerId: UUID,
        personident: String,
        deltakerlisteId: UUID,
    ) {
        /*
            Denne funksjonen er ment til alle scenarioer hvor det er relevant med låsing av deltakere.
            Skal kalles etter at databasen er oppdatert med ny/oppdatering av deltaker som gjør at den er vanskelig å gjenbruke noen steder
            (flere steder så oppdateres ikke databasen med deltakerendringer før resultatet er mottatt på kafka).
            OBS: Flere deltakelser kan ha samme påmeldt dato(i tilfelle den ene er historisert).
            Scenario 1: oppdatering av eksisterende deltakelser
            Scenario 2: Import av data fra arena
            Scenario 3: Avbryt utkast som i praksis vil ha en deltakelse uten påmeldtdato
         */
        val deltakelserPaaPerson = deltakerRepository
            .getMany(personident, deltakerlisteId)
            .sortedWith(
                compareByDescending<Deltaker> { it.paameldtDato }
                    .thenByDescending { it.status.gyldigFra },
            )

        if (deltakelserPaaPerson.none { it.id == deltakerId }) {
            throw IllegalStateException("Den nye deltakelsen $deltakerId må være upsertet for å bruke denne funksjonen")
        }

        val nyesteDeltakelse = deltakelserPaaPerson.firstOrNull { it.status.type in AKTIVE_STATUSER } ?: deltakelserPaaPerson.first()

        if (deltakerId != nyesteDeltakelse.id) {
            log.info("Fikk oppdatering på $deltakerId som skal låses fordi det er nyere deltakelse ${nyesteDeltakelse.id} på personen")
        }

        val deltakelserSomSkalLaases = deltakelserPaaPerson
            .filter {
                it.id != nyesteDeltakelse.id ||
                    nyesteDeltakelse.status.type == DeltakerStatus.Type.FEILREGISTRERT ||
                    nyesteDeltakelse.status.aarsak?.type == DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
            }.filter { it.kanEndres }

        val laasesMedAktivStatus = deltakelserSomSkalLaases
            .filter { it.status.type in AKTIVE_STATUSER }

        if (laasesMedAktivStatus.isNotEmpty()) {
            throw IllegalStateException(
                "ugyldig state. Fant eldre deltakelser med aktiv status: " +
                    "Nyeste deltaker ${nyesteDeltakelse.id} " +
                    "påmeldt ${nyesteDeltakelse.paameldtDato} " +
                    "har status ${nyesteDeltakelse.status.type}. " +
                    "Eldre deltakelse(r) ${laasesMedAktivStatus.map { it.id }} " +
                    "påmeldt ${laasesMedAktivStatus.map { it.paameldtDato }} " +
                    "har status ${laasesMedAktivStatus.map { it.status.type }}. ",
            )
        }

        if (!nyesteDeltakelse.kanEndres) {
            // Dette skal ikke skje i en ventet funksjonell flyt men mange feil med
            // låsing opp igjennom tidene har ført til at nyeste deltakelse er låst
            log.warn("Nyeste deltakelse ${nyesteDeltakelse.id} var låst for endringer. Låser opp..")
            deltakerRepository.settKanEndres(listOf(nyesteDeltakelse.id), true)
        }

        log.info("Låser ${deltakelserSomSkalLaases.size} deltakere for endringer pga nyere aktiv deltaker med id ${nyesteDeltakelse.id}")
        deltakerRepository.settKanEndres(deltakelserSomSkalLaases.map { it.id }, false)
    }

    private fun laasTidligereDeltakelser(deltakeroppdatering: Deltakeroppdatering) {
        if (deltakeroppdatering.status.type in AKTIVE_STATUSER && harEndretStatus(deltakeroppdatering)) {
            val tidligereDeltakelser = deltakerRepository.getTidligereAvsluttedeDeltakelser(deltakeroppdatering.id)
            deltakerRepository.settKanEndres(tidligereDeltakelser, false)
            log.info(
                "Har låst ${tidligereDeltakelser.size} deltakere for endringer pga nyere aktiv deltaker med id ${deltakeroppdatering.id}",
            )
        }
    }

    suspend fun oppdaterDeltaker(
        deltakeroppdatering: Deltakeroppdatering,
        isSynchronousInvocation: Boolean = true,
        afterUpsert: () -> Unit = {},
    ) {
        // CR-note: Burde det vært kastet en feil her hvis eksisterendeDeltaker er null?
        val eksisterendeDeltaker = deltakerRepository.get(deltakeroppdatering.id).getOrNull()

        val nyTid = deltakeroppdatering.status.opprettet.truncatedTo(ChronoUnit.MILLIS)

        val skalOppdatereStatus =
            isSynchronousInvocation ||
                eksisterendeDeltaker == null ||
                nyTid >= eksisterendeDeltaker.status.opprettet.truncatedTo(ChronoUnit.MILLIS)

        val disableKanEndres = deltakeroppdatering.status.type == DeltakerStatus.Type.FEILREGISTRERT ||
            deltakeroppdatering.status.aarsak?.type == DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT

        Database.transaction {
            laasTidligereDeltakelser(deltakeroppdatering)

            deltakerRepository.update(deltakeroppdatering)

            if (skalOppdatereStatus) {
                DeltakerStatusRepository.lagreStatus(deltakeroppdatering.id, deltakeroppdatering.status)
                DeltakerStatusRepository.deaktiverTidligereStatuser(deltakeroppdatering.id, deltakeroppdatering.status)
            }

            // deltakerRepository.settKanEndres kalles også i laasTidligereDeltakelser, undersøk
            if (disableKanEndres) {
                deltakerRepository.settKanEndres(listOf(deltakeroppdatering.id), false)
            }

            afterUpsert()
        }
    }

    private suspend fun slettKladd(deltakerlisteId: UUID, personident: String): Boolean {
        val kladd = deltakerRepository.getKladdForDeltakerliste(deltakerlisteId, personident)
        return kladd?.let { slettKladd(kladd) } == true
    }

    suspend fun slettKladd(deltaker: Deltaker): Boolean {
        if (deltaker.status.type != DeltakerStatus.Type.KLADD) {
            log.warn("Kan ikke slette deltaker med id ${deltaker.id} som har status ${deltaker.status.type}")
            return false
        }
        paameldingClient.slettKladd(deltaker.id)
        delete(deltaker.id)
        return true
    }

    suspend fun delete(deltakerId: UUID) = Database.transaction {
        forslagRepository.deleteForDeltaker(deltakerId)
        DeltakerStatusRepository.slettStatus(deltakerId)
        deltakerRepository.slettDeltaker(deltakerId)
    }

    // benyttes av Routing.registerInnbyggerApi
    suspend fun oppdaterSistBesokt(deltaker: Deltaker) {
        val sistBesokt = ZonedDateTime.now()
        amtDeltakerClient.sistBesokt(deltaker.id, sistBesokt)
        deltakerRepository.oppdaterSistBesokt(deltaker.id, sistBesokt)
    }

    // benyttes av TiltakskoordinatorService
    suspend fun oppdaterDeltakere(oppdaterteDeltakere: List<Deltakeroppdatering>) {
        Database.transaction {
            deltakerRepository.updateBatch(oppdaterteDeltakere)
            DeltakerStatusRepository.batchInsert(oppdaterteDeltakere)
            DeltakerStatusRepository.batchDeaktiverTidligereStatuser(oppdaterteDeltakere)
        }
    }

    private fun harEndretStatus(deltakeroppdatering: Deltakeroppdatering): Boolean {
        val currentStatus: DeltakerStatus =
            DeltakerStatusRepository.getDeltakerStatuser(deltakeroppdatering.id).first { it.gyldigTil == null }
        return currentStatus.type != deltakeroppdatering.status.type
    }
}
