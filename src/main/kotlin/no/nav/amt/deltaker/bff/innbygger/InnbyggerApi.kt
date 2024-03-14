package no.nav.amt.deltaker.bff.innbygger

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import no.nav.amt.deltaker.bff.application.plugins.getPersonident
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.innbygger.model.InnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.innbygger.model.toInnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import java.util.UUID

fun Routing.registerInnbyggerApi(
    deltakerService: DeltakerService,
    tilgangskontrollService: TilgangskontrollService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
) {
    fun komplettInnbyggerDeltakerResponse(deltaker: Deltaker): InnbyggerDeltakerResponse {
        val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
        val enhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }
        return deltaker.toInnbyggerDeltakerResponse(ansatte, enhet)
    }

    authenticate("INNBYGGER") {
        get("innbygger/{id}") {
            val innbygger = getPersonident()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["id"])).getOrThrow()
            tilgangskontrollService.verifiserInnbyggersTilgangTilDeltaker(innbygger, deltaker.navBruker.personident)

            call.respond(komplettInnbyggerDeltakerResponse(deltaker))
        }
    }
}
