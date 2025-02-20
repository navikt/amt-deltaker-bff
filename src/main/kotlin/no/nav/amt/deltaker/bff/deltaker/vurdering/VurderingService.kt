package no.nav.amt.deltaker.bff.deltaker.vurdering

import no.nav.amt.lib.models.arrangor.melding.Vurdering
import java.util.UUID

class VurderingService(
    private val vurderingRepository: VurderingRepository,
) {
    fun getSisteVurderingForDeltaker(deltakerId: UUID) = getForDeltaker(deltakerId).maxByOrNull { it.opprettet }

    fun getForDeltaker(deltakerId: UUID) = vurderingRepository.getForDeltaker(deltakerId)

    fun get(id: UUID) = vurderingRepository.get(id)

    fun upsert(vurderinger: List<Vurdering>) = vurderinger.forEach { vurdering -> upsert(vurdering) }

    fun upsert(vurdering: Vurdering) = vurderingRepository.upsert(vurdering)
}
