package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.AKTIVE_STATUSER
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val navEnhetService: NavEnhetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun get(id: UUID) = deltakerRepository.get(id)

    fun getDeltakelser(personident: String, deltakerlisteId: UUID) = deltakerRepository.getMany(personident, deltakerlisteId)

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
                amtDeltakerClient.endreBakgrunnsinformasjon(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    endring = endring,
                )
            }

            is DeltakerEndring.Endring.EndreInnhold -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreInnhold(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    innhold = Deltakelsesinnhold(
                        ledetekst = endring.ledetekst,
                        innhold = endring.innhold,
                    ),
                )
            }

            is DeltakerEndring.Endring.AvsluttDeltakelse -> endreDeltaker(deltaker) {
                amtDeltakerClient.avsluttDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                    aarsak = endring.aarsak,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreDeltakelsesmengde(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    deltakelsesprosent = endring.deltakelsesprosent,
                    dagerPerUke = endring.dagerPerUke,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.EndreSluttarsak -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreSluttaarsak(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    aarsak = endring.aarsak,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.EndreSluttdato -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreSluttdato(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.EndreStartdato -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreStartdato(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    startdato = endring.startdato,
                    sluttdato = endring.sluttdato,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.ForlengDeltakelse -> endreDeltaker(deltaker) {
                amtDeltakerClient.forlengDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.IkkeAktuell -> endreDeltaker(deltaker) {
                amtDeltakerClient.ikkeAktuell(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    aarsak = endring.aarsak,
                    begrunnelse = endring.begrunnelse,
                    forslagId = forslagId,
                )
            }

            is DeltakerEndring.Endring.ReaktiverDeltakelse -> endreDeltaker(deltaker) {
                amtDeltakerClient.reaktiverDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    begrunnelse = endring.begrunnelse,
                )
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

    fun oppdaterDeltaker(deltakeroppdatering: Deltakeroppdatering) {
        if (deltakeroppdatering.status.type in AKTIVE_STATUSER && harEndretStatus(deltakeroppdatering)) {
            val tidligereDeltakelser = deltakerRepository.getTidligereAvsluttedeDeltakelser(deltakeroppdatering.id)
            deltakerRepository.settKanIkkeEndres(tidligereDeltakelser)
            log.info(
                "Har låst ${tidligereDeltakelser.size} deltakere for endringer pga nyere aktiv deltaker med id ${deltakeroppdatering.id}",
            )
        }
        deltakerRepository.update(deltakeroppdatering)
        if (deltakeroppdatering.status.type == DeltakerStatus.Type.FEILREGISTRERT ||
            deltakeroppdatering.status.aarsak?.type == DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
        ) {
            deltakerRepository.settKanIkkeEndres(listOf(deltakeroppdatering.id))
        }
    }

    fun delete(deltakerId: UUID) {
        deltakerRepository.delete(deltakerId)
    }

    private fun upsertKladd(deltaker: Deltaker) {
        deltakerRepository.upsert(deltaker)
        log.info("Upserter kladd for deltaker med id ${deltaker.id}")
    }

    fun opprettDeltaker(kladd: KladdResponse): Result<Deltaker> {
        deltakerRepository.create(kladd)
        return deltakerRepository.get(kladd.id)
    }

    private fun harEndretStatus(deltakeroppdatering: Deltakeroppdatering): Boolean {
        val currentStatus = deltakerRepository.getDeltakerStatuser(deltakeroppdatering.id).first { it.gyldigTil == null }
        return currentStatus.type != deltakeroppdatering.status.type
    }

    suspend fun oppdaterSistBesokt(deltaker: Deltaker) {
        val sistBesokt = ZonedDateTime.now()
        amtDeltakerClient.sistBesokt(deltaker.id, sistBesokt)
        deltakerRepository.oppdaterSistBesokt(deltaker.id, sistBesokt)
    }
}

fun Deltaker.oppdater(oppdatering: Deltakeroppdatering) = this.copy(
    startdato = oppdatering.startdato,
    sluttdato = oppdatering.sluttdato,
    dagerPerUke = oppdatering.dagerPerUke,
    deltakelsesprosent = oppdatering.deltakelsesprosent,
    bakgrunnsinformasjon = oppdatering.bakgrunnsinformasjon,
    deltakelsesinnhold = oppdatering.deltakelsesinnhold,
    status = oppdatering.status,
    historikk = oppdatering.historikk,
)
