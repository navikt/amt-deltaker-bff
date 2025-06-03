package no.nav.amt.deltaker.bff.tiltakskoordinator.api

import io.ktor.http.ContentType
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.AuthorizationException
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerDetaljerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.VurderingResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.toResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import java.util.UUID

fun Routing.registerTiltakskoordinatorDeltakerApi(
    tiltakskoordinatorService: TiltakskoordinatorService,
    deltakerService: DeltakerService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    sporbarhetsloggService: SporbarhetsloggService,
) {
    val apiPath = "/tiltakskoordinator/deltaker/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        get(apiPath) {
            val deltakerId = UUID.fromString(call.parameters["id"])
            val tiltakskoordinatorsDeltaker = tiltakskoordinatorService.get(deltakerId)
            val deltakerlisteId = tiltakskoordinatorsDeltaker.deltakerliste.id
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            sporbarhetsloggService.sendAuditLog(
                navIdent = navIdent,
                deltakerPersonIdent = tiltakskoordinatorsDeltaker.navBruker.personident,
            )

            val tilgangTilBruker = tilgangskontrollService.harKoordinatorTilgangTilPerson(
                navAnsattAzureId,
                tiltakskoordinatorsDeltaker.navBruker,
            )

            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId)
            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
            call.respond(tiltakskoordinatorsDeltaker.toResponse(tilgangTilBruker))
        }

        get("$apiPath/historikk") {
            val deltakerId = UUID.fromString(call.parameters["id"])
            val deltaker = deltakerService.get(deltakerId).getOrThrow()

            val deltakerlisteId = deltaker.deltakerliste.id
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            sporbarhetsloggService.sendAuditLog(
                navIdent = navIdent,
                deltakerPersonIdent = deltaker.navBruker.personident,
            )

            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId)
            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)

            val tilgangTilBruker = tilgangskontrollService.harKoordinatorTilgangTilPerson(
                navAnsattAzureId,
                deltaker.navBruker,
            )
            if (!tilgangTilBruker) {
                throw AuthorizationException("Ansatt har ikke tilgang til å se historikken til deltaker $deltakerId")
            }

            val historikk = deltaker.getDeltakerHistorikkForVisning()
            val ansatte = navAnsattService.hentAnsatteForHistorikk(historikk)
            val enheter = navEnhetService.hentEnheterForHistorikk(historikk)
            val arrangornavn = deltaker.deltakerliste.arrangor.getArrangorNavn()

            val historikkResponse = historikk.toResponse(ansatte, arrangornavn, enheter, deltaker.deltakerliste.oppstart!!)
            val json = objectMapper.writePolymorphicListAsString(historikkResponse)
            call.respondText(json, ContentType.Application.Json)
        }
    }
}

fun TiltakskoordinatorsDeltaker.toResponse(tilgangTilBruker: Boolean): DeltakerDetaljerResponse {
    val (fornavn, mellomnavn, etternavn) = navBruker.getVisningsnavn(tilgangTilBruker)
    val personident = if (tilgangTilBruker) navBruker.personident else null

    return DeltakerDetaljerResponse(
        id = id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fodselsnummer = personident,
        status = status.toResponse(),
        startdato = startdato,
        sluttdato = sluttdato,
        navEnhet = navEnhet,
        navVeileder = navVeileder,
        beskyttelsesmarkering = beskyttelsesmarkering,
        vurdering = vurdering?.let {
            VurderingResponse(
                type = vurdering.vurderingstype,
                begrunnelse = vurdering.begrunnelse,
            )
        },
        innsatsgruppe = innsatsgruppe,
        tiltakskode = deltakerliste.tiltak.tiltakskode,
        tilgangTilBruker = tilgangTilBruker
    )
}
