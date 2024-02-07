package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.VedtakRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import java.util.UUID

class DeltakerHistorikkService(
    private val deltakerEndringRepository: DeltakerEndringRepository,
    private val vedtakRepository: VedtakRepository,
) {
    fun getForDeltaker(id: UUID): List<DeltakerHistorikk> {
        val deltakerHistorikk = deltakerEndringRepository.getForDeltaker(id).map { DeltakerHistorikk.Endring(it) }
        val vedtak = vedtakRepository.getForDeltaker(id)
            .filter { it.fattet != null }
            .map { DeltakerHistorikk.Vedtak(it) }

        val historikk = deltakerHistorikk
            .plus(vedtak)
            .sortedByDescending {
                when (it) {
                    is DeltakerHistorikk.Endring -> it.endring.endret
                    is DeltakerHistorikk.Vedtak -> it.vedtak.fattet
                }
            }

        return historikk
    }
}
