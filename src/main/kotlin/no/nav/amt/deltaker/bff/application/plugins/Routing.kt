package no.nav.amt.deltaker.bff.application.plugins

import io.getunleash.Unleash
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.auth.AuthenticationException
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.registerDeltakerApi
import no.nav.amt.deltaker.bff.deltaker.api.registerPameldingApi
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerForUngException
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteStengtException
import no.nav.amt.deltaker.bff.innbygger.InnbyggerService
import no.nav.amt.deltaker.bff.innbygger.registerInnbyggerApi
import no.nav.amt.deltaker.bff.internal.registerInternalApi
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.testdata.TestdataService
import no.nav.amt.deltaker.bff.testdata.registerTestdataApi
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.registerTiltakskoordinatorDeltakerApi
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.registerTiltakskoordinatorDeltakerlisteApi
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.unleash.registerUnleashApi
import no.nav.amt.lib.ktor.routing.registerHealthApi
import org.slf4j.Logger
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
    sporbarhetsloggService: SporbarhetsloggService,
    deltakerRepository: DeltakerRepository,
    amtDeltakerClient: AmtDeltakerClient,
    deltakerlisteService: DeltakerlisteService,
    unleash: Unleash,
    tiltakskoordinatorService: TiltakskoordinatorService,
    testdataService: TestdataService,
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
        exception<DeltakerlisteStengtException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.Gone, call, cause)
            call.respondText(text = "410: ${cause.message}", status = HttpStatusCode.Gone)
        }
        exception<Throwable> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.InternalServerError, call, cause)
            call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
        exception<DeltakerForUngException> { call, cause ->
            StatusPageLogger.log(HttpStatusCode.BadRequest, call, cause)
            call.respondText(text = "DELTAKER_FOR_UNG", status = HttpStatusCode.BadRequest)
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
            sporbarhetsloggService,
            UnleashToggle(unleash),
        )

        registerPameldingApi(
            tilgangskontrollService,
            deltakerService,
            pameldingService,
            navAnsattService,
            navEnhetService,
            forslagService,
            amtDistribusjonClient,
            deltakerlisteService,
        )

        registerInnbyggerApi(
            deltakerService,
            tilgangskontrollService,
            navAnsattService,
            navEnhetService,
            innbyggerService,
            forslagService,
        )

        registerInternalApi(
            deltakerRepository,
            amtDeltakerClient,
        )

        registerUnleashApi(
            unleash,
        )

        registerTiltakskoordinatorDeltakerlisteApi(
            deltakerlisteService,
            tilgangskontrollService,
            tiltakskoordinatorService,
        )

        registerTiltakskoordinatorDeltakerApi(
            tiltakskoordinatorService,
            deltakerService,
            navAnsattService,
            navEnhetService,
            deltakerlisteService,
            tilgangskontrollService,
            sporbarhetsloggService,
        )

        if (!Environment.isProd()) {
            registerTestdataApi(testdataService)
        }

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
    val log: Logger = LoggerFactory.getLogger(javaClass)

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
