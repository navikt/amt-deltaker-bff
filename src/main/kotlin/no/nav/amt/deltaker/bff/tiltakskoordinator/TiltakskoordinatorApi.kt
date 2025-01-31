package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerlisteResponse
import java.util.UUID

fun Routing.registerTiltakskoordinatorApi(
    deltakerService: DeltakerService,
    deltakerlisteRepository: DeltakerlisteRepository,
    tilgangskontrollService: TilgangskontrollService,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        if (!Environment.isProd()) {
            get(apiPath) {
                val deltakerlisteId = getDeltakerlisteId()
                val deltakerliste = deltakerlisteRepository.get(deltakerlisteId).getOrThrow()

                call.respond(deltakerliste.toResponse())
            }

            get("$apiPath/deltakere") {
                val deltakerlisteId = getDeltakerlisteId()

                tilgangskontrollService.verifiserTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId)

                val deltakere = deltakerService.getForDeltakerliste(deltakerlisteId)

                call.respond(deltakere.map { it.toDeltakerResponse() })
            }

            post("$apiPath/tilgang/legg-til") {
                val deltakerlisteId = getDeltakerlisteId()

                tilgangskontrollService.leggTilTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId).getOrThrow()

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun RoutingContext.getDeltakerlisteId(): UUID {
    val id = call.parameters["id"] ?: throw IllegalArgumentException("Påkrevd URL parameter 'deltakerlisteId' mangler.")

    return try {
        UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("URL parameter 'deltakerlisteId' er ikke formattert riktig.")
    }
}

fun Deltaker.toDeltakerResponse() = DeltakerResponse(
    id = this.id,
    fornavn = if (navBruker.erAdressebeskyttet) "" else this.navBruker.fornavn,
    mellomnavn = if (navBruker.erAdressebeskyttet) null else this.navBruker.mellomnavn,
    etternavn = if (navBruker.erAdressebeskyttet) "" else this.navBruker.etternavn,
    DeltakerResponse.DeltakerStatusResponse(
        type = this.status.type,
        aarsak = this.status.aarsak?.let {
            DeltakerResponse.DeltakerStatusAarsakResponse(
                it.type,
            )
        },
    ),
)

fun Deltakerliste.toResponse() = DeltakerlisteResponse(
    this.id,
    this.startDato,
    this.sluttDato,
    this.apentForPamelding,
    this.antallPlasser,
)
