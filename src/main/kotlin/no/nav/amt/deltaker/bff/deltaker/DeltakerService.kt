package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
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
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> opprinneligDeltaker.copy(
                bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
            )

            is DeltakerEndring.Endring.EndreInnhold -> opprinneligDeltaker.copy(
                innhold = endring.innhold,
            )

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> opprinneligDeltaker.copy(
                deltakelsesprosent = endring.deltakelsesprosent,
                dagerPerUke = endring.dagerPerUke,
            )

            is DeltakerEndring.Endring.EndreStartdato -> opprinneligDeltaker.copy(
                startdato = endring.startdato,
            )

            is DeltakerEndring.Endring.EndreSluttdato -> opprinneligDeltaker.copy(
                sluttdato = endring.sluttdato,
            )

            is DeltakerEndring.Endring.IkkeAktuell -> opprinneligDeltaker.copy(
                status = nyDeltakerStatus(DeltakerStatus.Type.IKKE_AKTUELL, endring.aarsak?.toDeltakerStatusAarsak()),
            )

            is DeltakerEndring.Endring.ForlengDeltakelse -> {
                if (opprinneligDeltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET &&
                    endring.sluttdato.isAfter(LocalDate.now())
                ) {
                    opprinneligDeltaker.copy(
                        status = nyDeltakerStatus(DeltakerStatus.Type.DELTAR),
                        sluttdato = endring.sluttdato,
                    )
                } else {
                    opprinneligDeltaker.copy(
                        sluttdato = endring.sluttdato,
                    )
                }
            }

            is DeltakerEndring.Endring.AvsluttDeltakelse -> opprinneligDeltaker.copy(
                status = nyDeltakerStatus(DeltakerStatus.Type.HAR_SLUTTET, endring.aarsak.toDeltakerStatusAarsak()),
                sluttdato = endring.sluttdato,
            )

            is DeltakerEndring.Endring.EndreSluttarsak -> opprinneligDeltaker.copy(
                status = nyDeltakerStatus(DeltakerStatus.Type.HAR_SLUTTET, endring.aarsak.toDeltakerStatusAarsak()),
            )
        }

        if (erEndret(opprinneligDeltaker, deltaker)) {
            val navAnsatt = navAnsattService.hentEllerOpprettNavAnsatt(endretAv)
            val navEnhet = endretAvEnhet.let { navEnhetService.hentEllerOpprettNavEnhet(it) }
            val deltakerEndring = DeltakerEndring(
                id = UUID.randomUUID(),
                deltakerId = deltaker.id,
                endring = endring,
                endretAv = navAnsatt.id,
                endretAvEnhet = navEnhet.id,
                endret = LocalDateTime.now(),
            )
            upsert(deltaker.copy(historikk = deltaker.historikk.plus(DeltakerHistorikk.Endring(deltakerEndring))))
            log.info("Oppdatert deltaker med id ${deltaker.id}")
        }
        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    fun oppdaterDeltaker(
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

    fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        status: DeltakerStatus,
    ): Deltaker {
        val deltaker = opprinneligDeltaker.copy(
            status = status,
        )

        upsert(deltaker)

        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    fun delete(deltakerId: UUID) {
        deltakerRepository.delete(deltakerId)
    }

    fun upsert(
        deltaker: Deltaker,
    ) {
        deltakerRepository.upsert(deltaker)
        log.info("Upserter deltaker med id ${deltaker.id}")
    }

    fun opprettDeltaker(kladd: KladdResponse): Result<Deltaker> {
        deltakerRepository.create(kladd)
        return deltakerRepository.get(kladd.id)
    }

    private fun erEndret(opprinneligDeltaker: Deltaker, oppdatertDeltaker: Deltaker): Boolean {
        return !(
            opprinneligDeltaker.bakgrunnsinformasjon == oppdatertDeltaker.bakgrunnsinformasjon &&
                opprinneligDeltaker.innhold == oppdatertDeltaker.innhold &&
                opprinneligDeltaker.deltakelsesprosent == oppdatertDeltaker.deltakelsesprosent &&
                opprinneligDeltaker.dagerPerUke == oppdatertDeltaker.dagerPerUke &&
                opprinneligDeltaker.startdato == oppdatertDeltaker.startdato &&
                opprinneligDeltaker.sluttdato == oppdatertDeltaker.sluttdato &&
                opprinneligDeltaker.status == oppdatertDeltaker.status
            )
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
