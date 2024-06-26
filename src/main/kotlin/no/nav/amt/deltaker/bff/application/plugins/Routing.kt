package no.nav.amt.deltaker.bff.application.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.amt.deltaker.bff.application.registerHealthApi
import no.nav.amt.deltaker.bff.auth.AuthenticationException
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.registerDeltakerApi
import no.nav.amt.deltaker.bff.deltaker.api.registerPameldingApi
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.innbygger.InnbyggerService
import no.nav.amt.deltaker.bff.innbygger.registerInnbyggerApi
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory

fun Application.configureRouting(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    pameldingService: PameldingService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    innbyggerService: InnbyggerService,
    forslagService: ForslagService,
    amtDistribusjonClient: AmtDistribusjonClient,
) {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.BadRequest, call, cause)
            call.respondText(text = "400: ${cause.message}", status = HttpStatusCode.BadRequest)
        }
        exception<AuthenticationException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.Forbidden, call, cause)
            call.respondText(text = "401: ${cause.message}", status = HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.Forbidden, call, cause)
            call.respondText(text = "403: ${cause.message}", status = HttpStatusCode.Forbidden)
        }
        exception<NoSuchElementException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.NotFound, call, cause)
            call.respondText(text = "404: ${cause.message}", status = HttpStatusCode.NotFound)
        }
        exception<Throwable> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.InternalServerError, call, cause)
            call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        registerHealthApi()

        registerDeltakerApi(
            tilgangskontrollService,
            deltakerService,
            navAnsattService,
            navEnhetService,
            forslagService,
            amtDistribusjonClient,
        )

        registerPameldingApi(
            tilgangskontrollService,
            deltakerService,
            pameldingService,
            navAnsattService,
            navEnhetService,
            forslagService,
            amtDistribusjonClient,
        )

        registerInnbyggerApi(
            deltakerService,
            tilgangskontrollService,
            navAnsattService,
            navEnhetService,
            innbyggerService,
            forslagService,
        )

        val catchAllRoute = "{...}"
        route(catchAllRoute) {
            handle {
                StatusPageLogger.log(
                    HttpStatusCode.NotFound,
                    this.call,
                    NoSuchElementException("Endepunktet eksisterer ikke"),
                )
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

object StatusPageLogger {
    val log = LoggerFactory.getLogger(javaClass)

    fun log(
        statusCode: HttpStatusCode,
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val msg = "${statusCode.value} ${statusCode.description}: " +
            "${call.request.httpMethod.value} ${call.request.path()}\n" +
            "Error: ${cause.message}"

        when (statusCode.value) {
            in 100..399 -> log.info(msg)
            in 400..404 -> log.warn(msg)
            else -> log.error(msg, cause)
        }
    }
}
