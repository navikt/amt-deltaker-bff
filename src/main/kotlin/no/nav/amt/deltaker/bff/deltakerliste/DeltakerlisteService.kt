package no.nav.amt.deltaker.bff.deltakerliste

import no.nav.amt.lib.ktor.clients.AmtPersonServiceClient
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class DeltakerlisteService(
    private val repository: DeltakerlisteRepository,
    private val amtPersonServiceClient: AmtPersonServiceClient,
) {
    companion object {
        const val GRUPPE_AMO_ALDERSGRENSE = 19

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

    suspend fun sjekkAldersgrenseForDeltakelse(deltakerlisteId: UUID, personident: String) {
        val deltakerliste = get(deltakerlisteId).getOrThrow()
        if (deltakerliste.tiltak.tiltakskode != Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
            return
        }

        val fodselsar = amtPersonServiceClient.hentNavBrukerFodselsar(personident)
        val dagensDato = LocalDate.now()

        if (deltakerliste.startDato.isBefore(dagensDato)) {
            // For kurstiltak med løpende oppstart så kan oppstartsdatoen på kurset være i fortiden når man melder på
            // og da må personen ha fylt 19 år på tidspunktet som man melder på
            if (dagensDato.year - fodselsar < GRUPPE_AMO_ALDERSGRENSE) {
                throw DeltakerForUngException("Deltaker er for ung for å delta på ${deltakerliste.tiltak.tiltakskode}")
            }
        } else if (deltakerliste.startDato.year - fodselsar < GRUPPE_AMO_ALDERSGRENSE) {
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
