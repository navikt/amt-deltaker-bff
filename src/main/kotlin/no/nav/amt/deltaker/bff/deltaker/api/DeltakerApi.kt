package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.model.ForslagTilDeltaker
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
) {
    authenticate("VEILEDER") {
        post("/pamelding") {
            val navIdent = getNavIdent()
            val pameldingRequest = call.receive<PameldingRequest>()
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), pameldingRequest.personident)
            val deltaker = deltakerService.opprettDeltaker(
                deltakerlisteId = pameldingRequest.deltakerlisteId,
                personident = pameldingRequest.personident,
                opprettetAv = navIdent,
            )
            call.respond(deltaker)
        }

        post("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val request = call.receive<ForslagRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"]))

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            deltakerService.opprettForslag(
                opprinneligDeltaker = deltaker,
                forslag = ForslagTilDeltaker(
                    mal = request.mal,
                    bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                    godkjentAvNav = null,
                ),
                endretAv = navIdent,
            )

            call.respond(HttpStatusCode.OK)
        }
    }
}
