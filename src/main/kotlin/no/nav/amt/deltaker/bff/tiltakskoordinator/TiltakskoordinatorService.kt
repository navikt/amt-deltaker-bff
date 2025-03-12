package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.oppdater
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import no.nav.amt.lib.models.tiltakskoordinator.response.EndringFraTiltakskoordinatorResponse
import java.util.UUID

class TiltakskoordinatorService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
    private val tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
) {
    suspend fun endreDeltakere(
        deltakerIder: List<UUID>,
        endring: EndringFraTiltakskoordinator.Endring,
        endretAv: String,
    ): List<Deltaker> {
        val oppdateringer = when (endring) {
            EndringFraTiltakskoordinator.DelMedArrangor -> amtDeltakerClient.delMedArrangor(deltakerIder, endretAv)
        }

        val deltakere = deltakerService.getMany(deltakerIder).associateBy { it.id }
        val oppdaterteDeltakere = oppdateringer.mapNotNull { oppdatering ->
            deltakere[oppdatering.id]?.oppdater(oppdatering)
        }

        deltakerService.oppdaterDeltakere(oppdaterteDeltakere)

        return oppdaterteDeltakere
    }

    fun hentKoordinatorer(deltakerlisteId: UUID) = tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)

    fun hentDeltakere(deltakerlisteId: UUID) = deltakerService
        .getForDeltakerliste(deltakerlisteId)
        .filterNot { deltaker -> deltaker.skalSkjules() }
}

fun Deltaker.oppdater(endring: EndringFraTiltakskoordinatorResponse) = this.copy(
    status = endring.status,
    sistEndret = endring.sistEndret,
)
