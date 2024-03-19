package no.nav.amt.deltaker.bff.innbygger

import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.oppdater

class InnbyggerService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
) {
    suspend fun fattVedtak(deltaker: Deltaker): Deltaker {
        val ikkeFattetVedtak = deltaker.ikkeFattetVedtak
        require(ikkeFattetVedtak != null) {
            "Deltaker ${deltaker.id} har ikke et vedtak som kan fattes"
        }

        val oppdatering = amtDeltakerClient.fattVedtak(ikkeFattetVedtak)

        deltakerService.oppdaterDeltaker(oppdatering, deltaker.navBruker.personident, deltaker.deltakerliste.id)

        return deltaker.oppdater(oppdatering)
    }
}
