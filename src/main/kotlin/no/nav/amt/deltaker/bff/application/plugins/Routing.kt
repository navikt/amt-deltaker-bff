package no.nav.amt.deltaker.bff.application.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import no.nav.amt.deltaker.bff.application.registerHealthApi
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.api.registerDeltakerApi
import no.nav.amt.deltaker.bff.deltaker.api.registerPameldingApi

fun Application.configureRouting(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    pameldingService: PameldingService,
) {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(text = "400: $cause", status = HttpStatusCode.BadRequest)
        }
        exception<AuthorizationException> { call, cause ->
            call.respondText(text = "403: $cause", status = HttpStatusCode.Forbidden)
        }
        exception<NoSuchElementException> { call, cause ->
            call.respondText(text = "404: $cause", status = HttpStatusCode.NotFound)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        registerHealthApi()

        registerDeltakerApi(tilgangskontrollService, deltakerService)
        registerPameldingApi(tilgangskontrollService, deltakerService, pameldingService)
    }
}
