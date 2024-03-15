package no.nav.amt.deltaker.bff.innbygger

import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.oppdater
import java.time.LocalDateTime

class InnbyggerService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
) {
    suspend fun fattVedtak(deltaker: Deltaker): Deltaker {
        val ikkeFattetVedtak = deltaker.ikkeFattetVedtak
        require(ikkeFattetVedtak != null) {
            "Deltaker ${deltaker.id} har ikke et vedtak som kan fattes"
        }

        val fattetVedtak = ikkeFattetVedtak.copy(
            fattet = LocalDateTime.now(),
            fattetAvNav = false,
            sistEndret = LocalDateTime.now(),
        )

        val oppdatering = amtDeltakerClient.fattVedtak(fattetVedtak)

        deltakerService.oppdaterDeltaker(oppdatering)

        return deltaker.oppdater(oppdatering)
    }
}
