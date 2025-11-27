package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.apiclients.DtoMappers.toDeltakerOppdatering
import no.nav.amt.deltaker.bff.apiclients.paamelding.PaameldingClient
import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.slf4j.LoggerFactory
import java.util.UUID

class PameldingService(
    private val deltakerService: DeltakerService,
    private val navBrukerService: NavBrukerService,
    private val paameldingClient: PaameldingClient,
    private val navEnhetService: NavEnhetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun opprettDeltaker(deltakerlisteId: UUID, personIdent: String): Deltaker {
        val eksisterendeDeltaker = deltakerService
            .getDeltakelser(personIdent, deltakerlisteId)
            .firstOrNull { !it.harSluttet() }

        if (eksisterendeDeltaker != null) {
            log.warn("Deltakeren ${eksisterendeDeltaker.id} er allerede opprettet og deltar fortsatt")
            return eksisterendeDeltaker
        }

        val kladdResponse = paameldingClient.opprettKladd(
            personIdent = personIdent,
            deltakerlisteId = deltakerlisteId,
        )

        navBrukerService.upsert(kladdResponse.navBruker)
        val deltaker = deltakerService.opprettKladd(kladdResponse).getOrThrow()

        MetricRegister.OPPRETTET_KLADD.inc()

        return deltaker
    }

    fun upsertKladd(kladd: Kladd): Deltaker? {
        if (kladd.opprinneligDeltaker.status.type !== DeltakerStatus.Type.KLADD) {
            // Dette kan skje når to brukere er inne på samme deltakelse samtidig
            // eller når samme bruker har flere faner med samme deltakelse
            // eller når nav veileder er så rask med å dele utkast at kladd requesten(som har en delay i frontend) kommer på etterskudd
            log.warn(
                "Kan ikke upserte kladd for deltaker ${kladd.opprinneligDeltaker.id} " +
                    "med status ${kladd.opprinneligDeltaker.status.type}," +
                    "status må være ${DeltakerStatus.Type.KLADD}.",
            )
            return null
        }

        return deltakerService.oppdaterKladd(
            kladd.opprinneligDeltaker,
            kladd.opprinneligDeltaker.status,
            kladd.pamelding,
        )
    }

    suspend fun upsertUtkast(utkast: Utkast): Deltaker {
        navEnhetService.hentOpprettEllerOppdaterNavEnhet(utkast.pamelding.endretAvEnhet)
        val deltakeroppdatering = paameldingClient.utkast(utkast).toDeltakerOppdatering()

        deltakerService.oppdaterDeltaker(deltakeroppdatering)
        return deltakerService.getDeltaker(utkast.deltakerId).getOrThrow()
    }

    suspend fun slettKladd(deltaker: Deltaker) = deltakerService.slettKladd(deltaker)

    suspend fun avbrytUtkast(
        deltaker: Deltaker,
        avbruttAvEnhet: String,
        avbruttAv: String,
    ) {
        navEnhetService.hentOpprettEllerOppdaterNavEnhet(avbruttAvEnhet)
        paameldingClient.avbrytUtkast(deltaker.id, avbruttAv, avbruttAvEnhet)

        val forrigeDeltaker = deltakerService
            .getDeltakelser(deltaker.navBruker.personident, deltaker.deltakerliste.id)
            .filter { it.id !== deltaker.id && it.paameldtDato != null }
            .sortedByDescending { it.paameldtDato }
            .firstOrNull() ?: return
        if (forrigeDeltaker.status.type != DeltakerStatus.Type.FEILREGISTRERT &&
            forrigeDeltaker.status.type != DeltakerStatus.Type.AVBRUTT_UTKAST &&
            forrigeDeltaker.status.aarsak?.type != DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
        ) {
            deltakerService.laasOppDeltaker(forrigeDeltaker)
        }
    }

    fun getKladder(personident: String): List<Deltaker> = deltakerService.getDeltakelser(personident).filter {
        it.status.type == DeltakerStatus.Type.KLADD
    }

    fun getKladderForDeltakerliste(deltakerlisteId: UUID): List<Deltaker> = deltakerService.getKladderForDeltakerliste(deltakerlisteId)
}
