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
        opprinneligDeltaker: Deltaker,
        endring: DeltakerEndring.Endring,
        endretAv: String,
        endretAvEnhet: String,
    ): Deltaker {
        val deltaker = when (endring) {
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon ->
                endreBakgrunnsinformasjon(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.EndreInnhold ->
                endreInnhold(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.AvsluttDeltakelse -> TODO()
            is DeltakerEndring.Endring.EndreDeltakelsesmengde ->
                endreDeltakelsesmengde(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.EndreSluttarsak ->
                endreSluttaarsak(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.EndreSluttdato ->
                endreSluttdato(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.EndreStartdato ->
                endreStartdato(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.ForlengDeltakelse -> TODO()
            is DeltakerEndring.Endring.IkkeAktuell -> TODO()
        }

        return deltaker
    }

    private suspend fun endreDeltaker(
        deltaker: Deltaker,
        amtDeltakerKall: suspend () -> Deltakeroppdatering,
    ): Deltaker {
        val deltakeroppdatering = amtDeltakerKall()
        oppdaterDeltaker(deltakeroppdatering)
        return deltaker.oppdater(deltakeroppdatering)
    }

    private suspend fun endreBakgrunnsinformasjon(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreBakgrunnsinformasjon,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreBakgrunnsinformasjon(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
        )
    }

    private suspend fun endreInnhold(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreInnhold,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreInnhold(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            innhold = endring.innhold,
        )
    }

    private suspend fun endreDeltakelsesmengde(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreDeltakelsesmengde,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreDeltakelsesmengde(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
        )
    }

    private suspend fun endreStartdato(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreStartdato,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreStartdato(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            startdato = endring.startdato,
        )
    }

    private suspend fun endreSluttdato(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreSluttdato,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreSluttdato(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            sluttdato = endring.sluttdato,
        )
    }

    private suspend fun endreSluttaarsak(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreSluttarsak,
    ) = endreDeltaker(deltaker) {
        amtDeltakerClient.endreSluttaarsak(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            aarsak = endring.aarsak,
        )
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
