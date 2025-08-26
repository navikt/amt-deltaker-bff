package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.apiclients.DtoMappers.toDeltakeroppdatering
import no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.apiclients.paamelding.PaameldingClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.AKTIVE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.AVSLUTTENDE_STATUSER
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

    fun get(id: UUID) = deltakerRepository.get(id)

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
        val oppdatertDeltaker = when (endring) {
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> endreDeltaker(deltaker) {
                amtDeltakerClient
                    .endreBakgrunnsinformasjon(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        endring = endring,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.EndreInnhold -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.AvsluttDeltakelse -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.EndreAvslutning -> endreDeltaker(deltaker) {
                amtDeltakerClient
                    .endreAvslutning(
                        deltakerId = deltaker.id,
                        endretAv = endretAv,
                        endretAvEnhet = endretAvEnhet,
                        aarsak = endring.aarsak,
                        begrunnelse = endring.begrunnelse,
                        harFullfort = endring.harFullfort,
                        forslagId = forslagId,
                    ).toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.AvbrytDeltakelse -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.EndreSluttarsak -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.EndreSluttdato -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.EndreStartdato -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.ForlengDeltakelse -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.IkkeAktuell -> endreDeltaker(deltaker) {
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

            is DeltakerEndring.Endring.ReaktiverDeltakelse -> endreDeltaker(deltaker) {
                val response = amtDeltakerClient.reaktiverDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    begrunnelse = endring.begrunnelse,
                )
                slettKladd(deltaker.deltakerliste.id, deltaker.navBruker.personident)
                response.toDeltakeroppdatering()
            }

            is DeltakerEndring.Endring.FjernOppstartsdato -> endreDeltaker(deltaker) {
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
        return oppdatertDeltaker
    }

    private suspend fun endreDeltaker(deltaker: Deltaker, amtDeltakerKall: suspend () -> Deltakeroppdatering): Deltaker {
        val deltakeroppdatering = amtDeltakerKall()
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

    fun opprettArenaDeltaker(deltaker: Deltaker) {
        val deltakelserPaSammeTiltak =
            getDeltakerIdOgStatusForDeltakelserPaTiltak(deltaker.navBruker.personident, deltaker.deltakerliste.id)
        deltakerRepository.upsert(deltaker)

        if (deltakelserPaSammeTiltak.isNotEmpty()) {
            // Det finnes tidligere deltakelser på samme tiltak
            val avsluttedeDeltakelserPaSammeTiltak =
                deltakelserPaSammeTiltak.filter { it.status.type in AVSLUTTENDE_STATUSER && it.kanEndres }
            if (deltaker.status.type in AKTIVE_STATUSER) {
                deltakerRepository.settKanEndres(avsluttedeDeltakelserPaSammeTiltak.map { it.id }, false)
                log.info(
                    "Har låst ${avsluttedeDeltakelserPaSammeTiltak.size} " +
                        "deltakere for endringer pga nyere aktiv deltaker fra arena med id ${deltaker.id}",
                )
            } else { // deltakelsen er gammel(er avsluttet)
                val aktiveDeltakelser = deltakelserPaSammeTiltak.filterNot { it.status.type in AVSLUTTENDE_STATUSER }
                if (aktiveDeltakelser.isNotEmpty()) {
                    deltakerRepository.settKanEndres(listOf(deltaker.id), false)
                    log.info(
                        "Har låst deltaker med id: ${deltaker.id} for endringer pga nyere aktive deltakelser",
                    )
                } else {
                    val nyereAvsluttetDeltakelse = avsluttedeDeltakelserPaSammeTiltak.find {
                        it.status.opprettet.isAfter(
                            deltaker.status.opprettet,
                        )
                    }
                    if (nyereAvsluttetDeltakelse != null) {
                        deltakerRepository.settKanEndres(listOf(deltaker.id), false)
                        log.info(
                            "Har låst deltaker med id: ${deltaker.id} for endringer pga nyere avsluttet deltakelse",
                        )
                    } else {
                        val skalIkkeKunneEndres = avsluttedeDeltakelserPaSammeTiltak.filterNot {
                            it.status.opprettet.isAfter(
                                deltaker.status.opprettet,
                            )
                        }
                        deltakerRepository.settKanEndres(skalIkkeKunneEndres.map { it.id }, false)
                        log.info(
                            "Har låst ${skalIkkeKunneEndres.size} " +
                                "deltakere for endringer pga nyere avsluttet deltaker fra arena med id ${deltaker.id}",
                        )
                    }
                }
            }
        }

        if (deltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT ||
            deltaker.status.aarsak?.type == DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
        ) {
            deltakerRepository.settKanEndres(listOf(deltaker.id), false)
        }
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

    fun opprettDeltaker(kladd: OpprettKladdResponse): Result<Deltaker> {
        deltakerRepository.create(kladd)
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

    private fun getDeltakerIdOgStatusForDeltakelserPaTiltak(personident: String, deltakerlisteId: UUID) =
        deltakerRepository.getDeltakerIdOgStatusForDeltakelser(personident, deltakerlisteId)

    fun getForDeltakerliste(deltakerlisteId: UUID) = deltakerRepository.getForDeltakerliste(deltakerlisteId)

    fun oppdaterDeltakere(oppdaterteDeltakere: List<Deltakeroppdatering>) = deltakerRepository.updateBatch(oppdaterteDeltakere)
}
