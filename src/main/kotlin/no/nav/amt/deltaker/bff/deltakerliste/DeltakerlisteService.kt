package no.nav.amt.deltaker.bff.deltakerliste

import java.util.UUID

class DeltakerlisteService(
    private val repository: DeltakerlisteRepository,
) {
    fun get(id: UUID) = repository.get(id)

    fun hentMedFellesOppstart(id: UUID) = repository.get(id).runCatching {
        val deltakerliste = this.getOrThrow()

        if (deltakerliste.getOppstartstype() == Deltakerliste.Oppstartstype.FELLES) {
            deltakerliste
        } else {
            throw NoSuchElementException("Deltakerliste ${deltakerliste.id} har ikke felles oppstart")
        }
    }

    fun verifiserDeltakerlisteHarFellesOppstart(id: UUID) = hentMedFellesOppstart(id).getOrThrow()
}
