package no.nav.amt.deltaker.bff.tiltakskoordinator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusAarsakResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerlisteResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.KoordinatorResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import java.util.UUID

fun Routing.registerTiltakskoordinatorDeltakerlisteApi(
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    tiltakskoordinatorService: TiltakskoordinatorService,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        get(apiPath) {
            val deltakerlisteId = getDeltakerlisteId()
            val deltakerliste = deltakerlisteService.hentMedFellesOppstart(deltakerlisteId).getOrThrow()
            val koordinatorer = tiltakskoordinatorService.hentKoordinatorer(deltakerlisteId)

            call.respond(deltakerliste.toResponse(koordinatorer))
        }

        get("$apiPath/deltakere") {
            val deltakerlisteId = getDeltakerlisteId()
            val navIdent = call.getNavIdent()

            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navIdent, deltakerlisteId)

            val navAnsattAzureId = call.getNavAnsattAzureId()

            val deltakere = tiltakskoordinatorService
                .hentDeltakere(deltakerlisteId)
                .map { deltaker ->
                    val harTilgang =
                        tilgangskontrollService.harKoordinatorTilgangTilPerson(navAnsattAzureId, deltaker.navBruker)
                    deltaker.toDeltakerResponse(harTilgang)
                }

            call.respond(deltakere)
        }

        post("$apiPath/deltakere/del-med-arrangor") {
            val deltakerlisteId = getDeltakerlisteId()
            val navIdent = call.getNavIdent()

            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navIdent, deltakerlisteId)

            val deltakerIder = call.receive<List<UUID>>()
            val oppdaterteDeltakere = tiltakskoordinatorService
                .endreDeltakere(
                    deltakerIder,
                    EndringFraTiltakskoordinator.DelMedArrangor,
                    navIdent,
                ).map {
                    val harTilgang =
                        tilgangskontrollService.harKoordinatorTilgangTilPerson(call.getNavAnsattAzureId(), it.navBruker)
                    it.toDeltakerResponse(harTilgang)
                }

            call.respond(oppdaterteDeltakere)
        }

        post("$apiPath/tilgang/legg-til") {
            val deltakerlisteId = getDeltakerlisteId()

            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId).getOrThrow()

            call.respond(HttpStatusCode.OK)
        }
    }
}

fun TiltakskoordinatorsDeltaker.toDeltakerResponse(harTilgang: Boolean): DeltakerResponse {
    val (fornavn, mellomnavn, etternavn) = navBruker.getVisningsnavn(harTilgang)

    return DeltakerResponse(
        id = id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        status = DeltakerStatusResponse(
            type = status.type,
            aarsak = status.aarsak?.let {
                DeltakerStatusAarsakResponse(
                    it.type,
                    it.beskrivelse,
                )
            },
        ),
        vurdering = vurdering?.vurderingstype,
        beskyttelsesmarkering = beskyttelsesmarkering,
        navEnhet = navEnhet,
        erManueltDeltMedArrangor = erManueltDeltMedArrangor,
    )
}

fun RoutingContext.getDeltakerlisteId(): UUID {
    val id =
        call.parameters["id"] ?: throw IllegalArgumentException("Påkrevd URL parameter 'deltakerlisteId' mangler.")

    return try {
        UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("URL parameter 'deltakerlisteId' er ikke formattert riktig.")
    }
}

fun Deltakerliste.toResponse(koordinatorer: List<NavAnsatt>) = DeltakerlisteResponse(
    this.id,
    this.tiltak.tiltakskode,
    this.startDato,
    this.sluttDato,
    this.apentForPamelding,
    this.antallPlasser,
    koordinatorer.map { KoordinatorResponse(id = it.id, navn = it.navn) },
)
