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
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.DeltakerResponseUtils
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusAarsakResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerlisteResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.KoordinatorResponse
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import java.util.UUID

fun Routing.registerTiltakskoordinatorDeltakerlisteApi(
    vurderingService: VurderingService,
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    navEnhetService: NavEnhetService,
    tiltakskoordinatorService: TiltakskoordinatorService,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    fun lagTiltakskoordinatorsDeltaker(deltaker: Deltaker, navAnsattAzureId: UUID): DeltakerResponseUtils {
        val harTilgang = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        return DeltakerResponseUtils(deltaker, harTilgang, sisteVurdering)
    }

    fun tilResponse(deltakere: List<DeltakerResponseUtils>): List<DeltakerResponse> {
        val navEnheter = navEnhetService.hentEnheter(deltakere.mapNotNull { it.deltaker.navBruker.navEnhetId })
        return deltakere.map { it.toDeltakerResponse(navEnheter[it.deltaker.navBruker.navEnhetId]) }
    }

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
                .map { lagTiltakskoordinatorsDeltaker(it, navAnsattAzureId) }

            call.respond(tilResponse(deltakere))
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
                ).map { lagTiltakskoordinatorsDeltaker(it, call.getNavAnsattAzureId()) }

            call.respond(tilResponse(oppdaterteDeltakere))
        }

        post("$apiPath/tilgang/legg-til") {
            val deltakerlisteId = getDeltakerlisteId()

            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId).getOrThrow()

            call.respond(HttpStatusCode.OK)
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

fun DeltakerResponseUtils.toDeltakerResponse(navEnhet: NavEnhet?): DeltakerResponse {
    val (fornavn, mellomnavn, etternavn) = visningsnavn()

    return DeltakerResponse(
        id = deltaker.id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        status = DeltakerStatusResponse(
            type = deltaker.status.type,
            aarsak = deltaker.status.aarsak?.let {
                DeltakerStatusAarsakResponse(
                    it.type,
                    it.beskrivelse,
                )
            },
        ),
        vurdering = vurdering?.vurderingstype,
        beskyttelsesmarkering = beskyttelsesmarkering(),
        navEnhet = navEnhet?.navn,
        erManueltDeltMedArrangor = deltaker.erManueltDeltMedArrangor,
    )
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
