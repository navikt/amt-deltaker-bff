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
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerTilgang
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerlisteResponse
import java.util.UUID

fun Routing.registerTiltakskoordinatorApi(
    deltakerService: DeltakerService,
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        if (!Environment.isProd()) {
            get(apiPath) {
                val deltakerlisteId = getDeltakerlisteId()
                val deltakerliste = deltakerlisteService.hentMedFellesOppstart(deltakerlisteId).getOrThrow()

                call.respond(deltakerliste.toResponse())
            }

            get("$apiPath/deltakere") {
                val deltakerlisteId = getDeltakerlisteId()

                deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
                tilgangskontrollService.verifiserTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId)

                val navAnsattAzureId = call.getNavAnsattAzureId()

                val deltakere = deltakerService
                    .getForDeltakerliste(deltakerlisteId)
                    .map { tilgangskontrollService.vurderKoordinatorTilgangTilDeltaker(navAnsattAzureId, it) }

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

fun TiltakskoordinatorDeltakerTilgang.toDeltakerResponse(): DeltakerResponse {
    val (fornavn, mellomnavn, etternavn) = visningsnavn()

    return DeltakerResponse(
        id = deltaker.id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        DeltakerResponse.DeltakerStatusResponse(
            type = deltaker.status.type,
            aarsak = deltaker.status.aarsak?.let {
                DeltakerResponse.DeltakerStatusAarsakResponse(
                    it.type,
                )
            },
        ),
        beskyttelsesmarkering = beskyttelsesmarkering(),
    )
}

fun Deltakerliste.toResponse() = DeltakerlisteResponse(
    this.id,
    this.startDato,
    this.sluttDato,
    this.apentForPamelding,
    this.antallPlasser,
)
