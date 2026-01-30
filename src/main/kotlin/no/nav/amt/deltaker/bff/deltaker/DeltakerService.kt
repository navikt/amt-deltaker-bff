package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.apiclients.DtoMappers.toDeltakeroppdatering
import no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.apiclients.paamelding.PaameldingClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.AKTIVE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.internalapis.paamelding.response.OpprettKladdResponse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val paameldingClient: PaameldingClient,
    private val navEnhetService: NavEnhetService,
    private val forslagService: ForslagService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getDeltaker(id: UUID) = deltakerRepository.get(id)

    fun getMany(ider: List<UUID>) = deltakerRepository.getMany(ider)

    fun getDeltakelser(personident: String, deltakerlisteId: UUID): List<Deltaker> =
        deltakerRepository.getMany(personident, deltakerlisteId)

    fun getDeltakelser(personident: String) = deltakerRepository.getMany(personident)

    fun getKladderForDeltakerliste(deltakerlisteId: UUID) = deltakerRepository.getKladderForDeltakerliste(deltakerlisteId)

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

    fun oppdaterKladd(
        opprinneligDeltaker: Deltaker,
        status: DeltakerStatus,
        endring: Pamelding,
    ): Deltaker {
        val deltaker = opprinneligDeltaker.copy(
            deltakelsesinnhold = endring.deltakelsesinnhold,
            bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
            status = status,
        )

        upsertKladd(deltaker)

        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    fun opprettDeltaker(deltaker: Deltaker) = deltakerRepository.upsert(deltaker)

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

    fun laasTidligereDeltakelser(deltakeroppdatering: Deltakeroppdatering) {
        if (deltakeroppdatering.status.type in AKTIVE_STATUSER && harEndretStatus(deltakeroppdatering)) {
            val tidligereDeltakelser = deltakerRepository.getTidligereAvsluttedeDeltakelser(deltakeroppdatering.id)
            deltakerRepository.settKanEndres(tidligereDeltakelser, false)
            log.info(
                "Har låst ${tidligereDeltakelser.size} deltakere for endringer pga nyere aktiv deltaker med id ${deltakeroppdatering.id}",
            )
        }
    }

    fun laasOppDeltaker(deltaker: Deltaker) {
        deltakerRepository.settKanEndres(listOf(deltaker.id), true)
        log.info(
            "Har låst opp tidligere deltaker ${deltaker.id} for endringer pga avbrutt utkast på nåværende deltaker",
        )
    }

    fun oppdaterDeltaker(deltakeroppdatering: Deltakeroppdatering, isSynchronousInvocation: Boolean = true) {
        laasTidligereDeltakelser(deltakeroppdatering)
        deltakerRepository.update(deltaker = deltakeroppdatering, isSynchronousInvocation = isSynchronousInvocation)

        if (deltakeroppdatering.status.type == DeltakerStatus.Type.FEILREGISTRERT ||
            deltakeroppdatering.status.aarsak?.type == DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
        ) {
            deltakerRepository.settKanEndres(listOf(deltakeroppdatering.id), false)
        }
    }

    suspend fun slettKladd(deltakerlisteId: UUID, personident: String): Boolean {
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

    fun delete(deltakerId: UUID) {
        forslagService.deleteForDeltaker(deltakerId)
        deltakerRepository.delete(deltakerId)
    }

    private fun upsertKladd(deltaker: Deltaker) {
        deltakerRepository.upsert(deltaker.copy(sistEndret = LocalDateTime.now()))
        log.info("Upserter kladd for deltaker med id ${deltaker.id}")
    }

    fun opprettKladd(kladd: OpprettKladdResponse): Result<Deltaker> {
        deltakerRepository.opprettKladd(kladd)
        return deltakerRepository.get(kladd.id)
    }

    private fun harEndretStatus(deltakeroppdatering: Deltakeroppdatering): Boolean {
        val currentStatus: DeltakerStatus = deltakerRepository.getDeltakerStatuser(deltakeroppdatering.id).first { it.gyldigTil == null }
        return currentStatus.type != deltakeroppdatering.status.type
    }

    suspend fun oppdaterSistBesokt(deltaker: Deltaker) {
        val sistBesokt = ZonedDateTime.now()
        amtDeltakerClient.sistBesokt(deltaker.id, sistBesokt)
        deltakerRepository.oppdaterSistBesokt(deltaker.id, sistBesokt)
    }

    fun getForDeltakerliste(deltakerlisteId: UUID) = deltakerRepository.getForDeltakerliste(deltakerlisteId)

    fun oppdaterDeltakere(oppdaterteDeltakere: List<Deltakeroppdatering>) = deltakerRepository.updateBatch(oppdaterteDeltakere)
}
