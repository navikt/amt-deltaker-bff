package no.nav.amt.deltaker.bff.innbygger

import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.oppdater
import no.nav.amt.lib.models.deltaker.DeltakerStatus

class InnbyggerService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
) {
    suspend fun godkjennUtkast(deltaker: Deltaker): Deltaker {
        require(deltaker.status.type == DeltakerStatus.Type.UTKAST_TIL_PAMELDING) {
            "Deltaker ${deltaker.id} har ikke status ${DeltakerStatus.Type.UTKAST_TIL_PAMELDING}"
        }

        val oppdatering = amtDeltakerClient.innbyggerGodkjennUtkast(deltaker.id)

        deltakerService.oppdaterDeltaker(oppdatering)

        return deltaker.oppdater(oppdatering)
    }
}
