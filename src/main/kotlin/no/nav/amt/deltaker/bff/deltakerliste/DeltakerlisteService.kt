package no.nav.amt.deltaker.bff.deltakerliste

import no.nav.amt.deltaker.bff.utils.Personident
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class DeltakerlisteService(
    private val repository: DeltakerlisteRepository,
) {
    companion object {
        const val GRUPPE_FAG_OG_YRKESOPPLAERING_ALDERSGRENSE = 19

        val tiltakskoordinatorGraceperiode: Period = Period.ofDays(14)
    }

    fun get(id: UUID) = repository.get(id)

    fun hentMedFellesOppstart(id: UUID) = repository.get(id).runCatching {
        val deltakerliste = this.getOrThrow()

        if (deltakerliste.getOppstartstype() == Deltakerliste.Oppstartstype.FELLES) {
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

    fun sjekkAldersgrenseForDeltakelse(deltakerlisteId: UUID, personident: String) {
        val deltakerliste = get(deltakerlisteId).getOrThrow()
        if (deltakerliste.tiltak.tiltakskode != Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING &&
            deltakerliste.tiltak.tiltakskode != Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING
        ) {
            return
        }

        val fodselsdato = Personident(personident).fodselsdato()

        if (deltakerliste.startDato.year - fodselsdato.year < GRUPPE_FAG_OG_YRKESOPPLAERING_ALDERSGRENSE) {
            throw DeltakerForUngException("Deltaker er for ung for å delta på ${deltakerliste.tiltak.tiltakskode}")
        }
    }
}

class DeltakerlisteStengtException(
    message: String? = null,
) : RuntimeException(message)

class DeltakerForUngException(
    message: String? = null,
) : RuntimeException(message)
