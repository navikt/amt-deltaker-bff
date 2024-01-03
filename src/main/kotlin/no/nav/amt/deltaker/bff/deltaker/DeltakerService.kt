package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerHistorikkRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerHistorikk
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DeltakerService(
    private val deltakerRepository: DeltakerRepository,
    private val deltakerlisteRepository: DeltakerlisteRepository,
    private val historikkRepository: DeltakerHistorikkRepository,
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
    private val deltakerProducer: DeltakerProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettDeltaker(
        deltakerlisteId: UUID,
        personident: String,
        opprettetAv: String,
        opprettetAvEnhet: String?,
    ): Deltaker {
        val deltakerliste = deltakerlisteRepository.get(deltakerlisteId).getOrThrow()

        val eksisterendeDeltaker = deltakerRepository.get(personident, deltakerlisteId).getOrNull()

        if (eksisterendeDeltaker != null && !eksisterendeDeltaker.harSluttet()) {
            log.warn("Deltakeren er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker
        }
        val deltaker = nyKladd(personident, deltakerliste, opprettetAv, opprettetAvEnhet)

        upsert(deltaker)

        return deltakerRepository.get(deltaker.id)
            .getOrThrow()
    }

    fun get(id: UUID) = deltakerRepository.get(id)

    suspend fun oppdaterDeltaker(
        opprinneligDeltaker: Deltaker,
        endringType: DeltakerEndringType,
        endring: DeltakerEndring,
        endretAv: String,
        endretAvEnhet: String?,
    ): Deltaker {
        if (opprinneligDeltaker.harSluttet()) {
            log.warn("Kan ikke endre på deltaker med id ${opprinneligDeltaker.id}, deltaker har sluttet")
            throw IllegalArgumentException("Kan ikke endre deltaker som har sluttet")
        }
        val deltaker = when (endring) {
            is DeltakerEndring.EndreBakgrunnsinformasjon -> opprinneligDeltaker.copy(
                bakgrunnsinformasjon = endring.bakgrunnsinformasjon,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.EndreMal -> opprinneligDeltaker.copy(
                mal = endring.mal,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.EndreDeltakelsesmengde -> opprinneligDeltaker.copy(
                deltakelsesprosent = endring.deltakelsesprosent,
                dagerPerUke = endring.dagerPerUke,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.EndreStartdato -> opprinneligDeltaker.copy(
                startdato = endring.startdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )

            is DeltakerEndring.EndreSluttdato -> opprinneligDeltaker.copy(
                sluttdato = endring.sluttdato,
                sistEndretAv = endretAv,
                sistEndretAvEnhet = endretAvEnhet,
                sistEndret = LocalDateTime.now(),
            )
        }

        if (erEndret(opprinneligDeltaker, deltaker)) {
            upsert(deltaker)
            historikkRepository.upsert(
                DeltakerHistorikk(
                    id = UUID.randomUUID(),
                    deltakerId = deltaker.id,
                    endringType = endringType,
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

    private suspend fun upsert(
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

    private fun nyKladd(
        personident: String,
        deltakerliste: Deltakerliste,
        opprettetAv: String,
        opprettetAvEnhet: String?,
    ): Deltaker =
        Deltaker(
            id = UUID.randomUUID(),
            personident = personident,
            deltakerliste = deltakerliste,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            bakgrunnsinformasjon = null,
            mal = emptyList(),
            status = nyDeltakerStatus(DeltakerStatus.Type.KLADD),
            sistEndretAv = opprettetAv,
            sistEndret = LocalDateTime.now(),
            sistEndretAvEnhet = opprettetAvEnhet,
            opprettet = LocalDateTime.now(),
        )
}

fun nyDeltakerStatus(type: DeltakerStatus.Type, aarsak: DeltakerStatus.Aarsak? = null) = DeltakerStatus(
    id = UUID.randomUUID(),
    type = type,
    aarsak = aarsak,
    gyldigFra = LocalDateTime.now(),
    gyldigTil = null,
    opprettet = LocalDateTime.now(),
)
