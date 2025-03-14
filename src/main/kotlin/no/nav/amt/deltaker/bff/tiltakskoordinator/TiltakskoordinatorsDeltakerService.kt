package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Kontaktinformasjon
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.NavVeileder
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import java.util.UUID

class TiltakskoordinatorsDeltakerService(
    val deltakerService: DeltakerService,
    val navAnsattService: NavAnsattService,
    val vurderingService: VurderingService,
    val navEnhetService: NavEnhetService,
    val tilgangskontrollService: TilgangskontrollService,
) {
    suspend fun get(deltakerId: UUID): TiltakskoordinatorsDeltaker {
        val deltaker = deltakerService.get(deltakerId).getOrThrow()
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        val navVeileder = deltaker.navBruker.navVeilederId?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }
        val navEnhet = deltaker.navBruker.navEnhetId?.let { navEnhetService.hentEnhet(it) }

        return TiltakskoordinatorsDeltaker(
            id = deltaker.id,
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = deltaker.status,
            startdato = deltaker.startdato,
            sluttdato = deltaker.sluttdato,
            kontaktinformasjon = Kontaktinformasjon(
                telefonnummer = null,
                epost = null,
                adresse = null,
            ),
            navEnhet = navEnhet?.navn,
            navVeileder = NavVeileder(
                navn = navVeileder?.navn,
                telefonnummer = null,
                epost = null,
            ),
            beskyttelsesmarkering = deltaker.navBruker.getBeskyttelsesmarkeringer(),
            vurdering = sisteVurdering,
            innsatsgruppe = deltaker.navBruker.innsatsgruppe,
        )
    }
}
