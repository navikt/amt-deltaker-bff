package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerSamtykkeRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerHistorikk
import java.util.UUID

class DeltakerHistorikkService(
    private val deltakerEndringRepository: DeltakerEndringRepository,
    private val samtykkeRepository: DeltakerSamtykkeRepository,
) {
    fun getForDeltaker(id: UUID): List<DeltakerHistorikk> {
        val deltakerHistorikk = deltakerEndringRepository.getForDeltaker(id).map { DeltakerHistorikk.Endring(it) }
        val samtykker = samtykkeRepository.getForDeltaker(id)
            .filter { it.godkjent != null }
            .map { DeltakerHistorikk.Samtykke(it) }

        val historikk = deltakerHistorikk
            .plus(samtykker)
            .sortedByDescending {
                when (it) {
                    is DeltakerHistorikk.Endring -> it.endring.endret
                    is DeltakerHistorikk.Samtykke -> it.samtykke.godkjent
                }
            }

        return historikk
    }
}
