package no.nav.amt.deltaker.bff.deltakerliste

import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class DeltakerlisteService(
    private val repository: DeltakerlisteRepository,
) {
    companion object {
        val tiltakskoordinatorGraceperiode: Period = Period.ofDays(14)
    }

    fun get(id: UUID) = repository.get(id)

    fun hentMedFellesOppstart(id: UUID) = repository.get(id).runCatching {
        val deltakerliste = this.getOrThrow()

        if (deltakerliste.getOppstartstype() == Oppstartstype.FELLES) {
            deltakerliste
        } else {
            throw NoSuchElementException("Deltakerliste ${deltakerliste.id} har ikke felles oppstart")
        }
    }

    fun verifiserTilgjengeligDeltakerliste(id: UUID): Deltakerliste {
        val deltakerliste = hentMedFellesOppstart(id).getOrThrow()

        deltakerliste.sluttDato?.let { sluttdato ->
            if (LocalDate.now().isAfter(sluttdato.plus(tiltakskoordinatorGraceperiode))) {
                throw DeltakerlisteStengtException("Deltakerlisten $id er stengt for tiltakskoordinator")
            }
        }

        return deltakerliste
    }
}
