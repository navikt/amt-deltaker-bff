package no.nav.amt.deltaker.bff.deltaker.forslag

import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.util.UUID

class ForslagService(
    private val forslagRepository: ForslagRepository,
) {
    fun getForDeltaker(deltakerId: UUID) = forslagRepository.getForDeltaker(deltakerId)

    fun upsert(forslag: Forslag) = forslagRepository.upsert(forslag)

    fun delete(id: UUID) = forslagRepository.delete(id)
}
