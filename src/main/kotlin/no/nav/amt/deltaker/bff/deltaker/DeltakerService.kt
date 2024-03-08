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

            is DeltakerEndring.Endring.AvsluttDeltakelse -> TODO()
            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> TODO()
            is DeltakerEndring.Endring.EndreInnhold -> TODO()
            is DeltakerEndring.Endring.EndreSluttarsak -> TODO()
            is DeltakerEndring.Endring.EndreSluttdato -> TODO()
            is DeltakerEndring.Endring.EndreStartdato -> TODO()
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
