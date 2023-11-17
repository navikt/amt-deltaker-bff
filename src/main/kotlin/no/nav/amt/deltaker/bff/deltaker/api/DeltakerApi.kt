package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
) {
    authenticate("VEILEDER") {
        post("/pamelding") {
            val pameldingRequest = call.receive<PameldingRequest>()
            val navAnsattAzureId = getNavAnsattAzureId()
            tilgangskontrollService.verifiserSkrivetilgang(navAnsattAzureId, pameldingRequest.personident)
            val deltaker = deltakerService.opprettDeltaker(
                deltakerlisteId = pameldingRequest.deltakerlisteId,
                personident = pameldingRequest.personident,
                opprettetAv = navAnsattAzureId.toString(), // må hente nav-ident
            )
            call.respond(deltaker)
        }
    }
}
