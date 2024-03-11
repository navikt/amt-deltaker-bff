package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
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

            is DeltakerEndring.Endring.EndreSluttarsak -> TODO()
            is DeltakerEndring.Endring.EndreSluttdato ->
                endreSluttdato(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.EndreStartdato ->
                endreStartdato(opprinneligDeltaker, endretAv, endretAvEnhet, endring)

            is DeltakerEndring.Endring.ForlengDeltakelse -> TODO()
            is DeltakerEndring.Endring.IkkeAktuell -> TODO()
        }

        return deltaker
    }

    private suspend fun endreBakgrunnsinformasjon(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreBakgrunnsinformasjon,
    ): Deltaker {
        amtDeltakerClient.endreBakgrunnsinformasjon(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
        )

        return deltaker.copy(bakgrunnsinformasjon = endring.bakgrunnsinformasjon)
    }

    private suspend fun endreInnhold(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreInnhold,
    ): Deltaker {
        amtDeltakerClient.endreInnhold(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            innhold = endring.innhold,
        )

        return deltaker.copy(innhold = endring.innhold)
    }

    private suspend fun endreDeltakelsesmengde(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreDeltakelsesmengde,
    ): Deltaker {
        amtDeltakerClient.endreDeltakelsesmengde(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
        )

        return deltaker.copy(
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
        )
    }

    private suspend fun endreStartdato(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreStartdato,
    ): Deltaker {
        amtDeltakerClient.endreStartdato(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            startdato = endring.startdato,
        )

        return deltaker.copy(startdato = endring.startdato)
    }

    private suspend fun endreSluttdato(
        deltaker: Deltaker,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreSluttdato,
    ): Deltaker {
        amtDeltakerClient.endreSluttdato(
            deltakerId = deltaker.id,
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            sluttdato = endring.sluttdato,
        )

        return deltaker.copy(sluttdato = endring.sluttdato)
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

fun nyDeltakerStatus(type: DeltakerStatus.Type, aarsak: DeltakerStatus.Aarsak? = null) = DeltakerStatus(
    id = UUID.randomUUID(),
    type = type,
    aarsak = aarsak,
    gyldigFra = LocalDateTime.now(),
    gyldigTil = null,
    opprettet = LocalDateTime.now(),
)
