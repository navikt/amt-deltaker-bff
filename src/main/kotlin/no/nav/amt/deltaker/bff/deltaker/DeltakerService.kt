package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerHistorikkRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.Endring
import no.nav.amt.deltaker.bff.deltaker.model.deltakerendring.Endringstype
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val historikkRepository: DeltakerHistorikkRepository,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
    private val deltakerProducer: DeltakerProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun get(id: UUID) = deltakerRepository.get(id)

    fun get(personident: String, deltakerlisteId: UUID) = deltakerRepository.get(personident, deltakerlisteId)

    suspend fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        endringstype: Endringstype,
        endring: Endring,
        endretAv: String,
        endretAvEnhet: String?,
    ): Deltaker {
        if (opprinneligDeltaker.harSluttet()) {
            log.warn("Kan ikke endre på deltaker med id ${opprinneligDeltaker.id}, deltaker har sluttet")
            throw IllegalArgumentException("Kan ikke endre deltaker som har sluttet")
        }
        val deltaker = when (endring) {
            is Endring.EndreBakgrunnsinformasjon -> opprinneligDeltaker.copy(
                bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is Endring.EndreMal -> opprinneligDeltaker.copy(
                mal = endring.mal,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is Endring.EndreDeltakelsesmengde -> opprinneligDeltaker.copy(
                deltakelsesprosent = endring.deltakelsesprosent,
                dagerPerUke = endring.dagerPerUke,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is Endring.EndreStartdato -> opprinneligDeltaker.copy(
                startdato = endring.startdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is Endring.EndreSluttdato -> opprinneligDeltaker.copy(
                sluttdato = endring.sluttdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )
        }

        if (erEndret(opprinneligDeltaker, deltaker)) {
            upsert(deltaker)
            historikkRepository.upsert(
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
        endring: OppdatertDeltaker,
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
                opprinneligDeltaker.sluttdato == oppdatertDeltaker.sluttdato
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
