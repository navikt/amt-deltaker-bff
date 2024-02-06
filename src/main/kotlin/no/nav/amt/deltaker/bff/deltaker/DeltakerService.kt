package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
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
    private val deltakerEndringRepository: DeltakerEndringRepository,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
    private val deltakerProducer: DeltakerProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun get(id: UUID) = deltakerRepository.get(id)

    fun getDeltakelser(personident: String, deltakerlisteId: UUID) =
        deltakerRepository.getMany(personident, deltakerlisteId)

    suspend fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        endringstype: DeltakerEndring.Endringstype,
        endring: DeltakerEndring.Endring,
        endretAv: String,
        endretAvEnhet: String?,
    ): Deltaker {
        val deltaker = when (endring) {
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> opprinneligDeltaker.copy(
                bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.EndreMal -> opprinneligDeltaker.copy(
                mal = endring.mal,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> opprinneligDeltaker.copy(
                deltakelsesprosent = endring.deltakelsesprosent,
                dagerPerUke = endring.dagerPerUke,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.EndreStartdato -> opprinneligDeltaker.copy(
                startdato = endring.startdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.EndreSluttdato -> opprinneligDeltaker.copy(
                sluttdato = endring.sluttdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.IkkeAktuell -> opprinneligDeltaker.copy(
                status = nyDeltakerStatus(DeltakerStatus.Type.IKKE_AKTUELL, endring.aarsak?.toDeltakerStatusAarsak()),
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.Endring.ForlengDeltakelse -> {
                if (opprinneligDeltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET && endring.sluttdato.isAfter(LocalDate.now())) {
                    opprinneligDeltaker.copy(
                        status = nyDeltakerStatus(DeltakerStatus.Type.DELTAR),
                        sluttdato = endring.sluttdato,
                        sistEndretAv = endretAv,
                        sistEndretAvEnhet = endretAvEnhet,
                        sistEndret = LocalDateTime.now(),
                    )
                } else {
                    opprinneligDeltaker.copy(
                        sluttdato = endring.sluttdato,
                        sistEndretAv = endretAv,
                        sistEndretAvEnhet = endretAvEnhet,
                        sistEndret = LocalDateTime.now(),
                    )
                }
            }
        }

        if (erEndret(opprinneligDeltaker, deltaker)) {
            upsert(deltaker)
            deltakerEndringRepository.upsert(
                DeltakerEndring(
                    id = UUID.randomUUID(),
                    deltakerId = deltaker.id,
                    endringstype = endringstype,
                    endring = endring,
                    endretAv = endretAv,
                    endretAvEnhet = endretAvEnhet,
                    endret = LocalDateTime.now(),
                ),
            )
            log.info("Oppdatert deltaker med id ${deltaker.id}")
        }
        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    suspend fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        status: DeltakerStatus,
        endring: Pamelding,
    ): Deltaker {
        val deltaker = opprinneligDeltaker.copy(
            mal = endring.mal,
            bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
            status = status,
            sistEndretAv = endring.endretAv,
            sistEndretAvEnhet = endring.endretAvEnhet,
            sistEndret = LocalDateTime.now(),
        )

        upsert(deltaker)

        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    suspend fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        status: DeltakerStatus,
        endretAvEnhet: String?,
        navIdent: String,
    ): Deltaker {
        val deltaker = opprinneligDeltaker.copy(
            status = status,
            sistEndretAv = navIdent,
            sistEndretAvEnhet = endretAvEnhet ?: opprinneligDeltaker.sistEndretAvEnhet,
            sistEndret = LocalDateTime.now(),
        )

        upsert(deltaker)

        return deltakerRepository.get(deltaker.id).getOrThrow()
    }

    fun delete(deltakerId: UUID) {
        deltakerRepository.delete(deltakerId)
    }

    suspend fun upsert(
        deltaker: Deltaker,
    ) {
        navAnsattService.hentEllerOpprettNavAnsatt(deltaker.sistEndretAv)
        deltaker.sistEndretAvEnhet?.let { navEnhetService.hentEllerOpprettNavEnhet(it) }

        deltakerRepository.upsert(deltaker)
        deltakerProducer.produce(deltaker)
        log.info("Upserter deltaker med id ${deltaker.id}")
    }

    private fun erEndret(opprinneligDeltaker: Deltaker, oppdatertDeltaker: Deltaker): Boolean {
        return !(
            opprinneligDeltaker.bakgrunnsinformasjon == oppdatertDeltaker.bakgrunnsinformasjon &&
                opprinneligDeltaker.mal == oppdatertDeltaker.mal &&
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
