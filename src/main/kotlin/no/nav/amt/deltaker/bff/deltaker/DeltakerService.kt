package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import org.slf4j.LoggerFactory
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val amtDeltakerClient: AmtDeltakerClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun get(id: UUID) = deltakerRepository.get(id)

    fun getDeltakelser(personident: String, deltakerlisteId: UUID) =
        deltakerRepository.getMany(personident, deltakerlisteId)

    suspend fun oppdaterDeltaker(
        deltaker: Deltaker,
        endring: DeltakerEndring.Endring,
        endretAv: String,
        endretAvEnhet: String,
    ): Deltaker {
        val oppdatertDeltaker = when (endring) {
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreBakgrunnsinformasjon(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
                )
            }

            is DeltakerEndring.Endring.EndreInnhold -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreInnhold(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    innhold = endring.innhold,
                )
            }

            is DeltakerEndring.Endring.AvsluttDeltakelse -> endreDeltaker(deltaker) {
                amtDeltakerClient.avsluttDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                    aarsak = endring.aarsak,
                )
            }

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreDeltakelsesmengde(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    deltakelsesprosent = endring.deltakelsesprosent,
                    dagerPerUke = endring.dagerPerUke,
                )
            }

            is DeltakerEndring.Endring.EndreSluttarsak -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreSluttaarsak(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    aarsak = endring.aarsak,
                )
            }

            is DeltakerEndring.Endring.EndreSluttdato -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreSluttdato(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                )
            }

            is DeltakerEndring.Endring.EndreStartdato -> endreDeltaker(deltaker) {
                amtDeltakerClient.endreStartdato(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    startdato = endring.startdato,
                )
            }

            is DeltakerEndring.Endring.ForlengDeltakelse -> endreDeltaker(deltaker) {
                amtDeltakerClient.forlengDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    sluttdato = endring.sluttdato,
                )
            }

            is DeltakerEndring.Endring.IkkeAktuell -> endreDeltaker(deltaker) {
                amtDeltakerClient.ikkeAktuell(
                    deltakerId = deltaker.id,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    aarsak = endring.aarsak,
                )
            }
        }
        return oppdatertDeltaker
    }

    private suspend fun endreDeltaker(
        deltaker: Deltaker,
        amtDeltakerKall: suspend () -> Deltakeroppdatering,
    ): Deltaker {
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
            innhold = endring.innhold,
            bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
            status = status,
        )

        upsert(deltaker)

        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    fun oppdaterDeltaker(deltakeroppdatering: Deltakeroppdatering) {
        deltakerRepository.update(deltakeroppdatering)
    }

    fun delete(deltakerId: UUID) {
        deltakerRepository.delete(deltakerId)
    }

    fun upsert(deltaker: Deltaker) {
        deltakerRepository.upsert(deltaker)
        log.info("Upserter deltaker med id ${deltaker.id}")
    }

    fun opprettDeltaker(kladd: KladdResponse): Result<Deltaker> {
        deltakerRepository.create(kladd)
        return deltakerRepository.get(kladd.id)
    }
}

private fun Deltaker.oppdater(oppdatering: Deltakeroppdatering) = this.copy(
    startdato = oppdatering.startdato,
    sluttdato = oppdatering.sluttdato,
    dagerPerUke = oppdatering.dagerPerUke,
    deltakelsesprosent = oppdatering.deltakelsesprosent,
    bakgrunnsinformasjon = oppdatering.bakgrunnsinformasjon,
    innhold = oppdatering.innhold,
    status = oppdatering.status,
    historikk = oppdatering.historikk,
)
