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
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.extensions.toResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.UlestHendelseService
import no.nav.amt.lib.ktor.auth.exceptions.AuthorizationException
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

fun Routing.registerTiltakskoordinatorDeltakerApi(
    tiltakskoordinatorService: TiltakskoordinatorService,
    deltakerService: DeltakerService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    sporbarhetsloggService: SporbarhetsloggService,
    ulesteHendelserService: UlestHendelseService,
) {
    val apiPath = "/tiltakskoordinator/deltaker/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        get(apiPath) {
            val deltakerId = UUID.fromString(call.parameters["id"])
            val navIdent = call.getNavIdent()
            val navAnsattAzureId = call.getNavAnsattAzureId()

            val tiltakskoordinatorsDeltaker = tiltakskoordinatorService.get(deltakerId)
            val deltakerListeId = tiltakskoordinatorsDeltaker.deltakerliste.id

            sporbarhetsloggService.sendAuditLog(
                navIdent = navIdent,
                deltakerPersonIdent = tiltakskoordinatorsDeltaker.navBruker.personident,
            )

            val harTilgangTilBruker = tilgangskontrollService.harKoordinatorTilgangTilPerson(
                navAnsattAzureId = navAnsattAzureId,
                navBruker = tiltakskoordinatorsDeltaker.navBruker,
            )

            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(
                navIdent = navIdent,
                deltakerlisteId = deltakerListeId,
            )

            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerListeId)

            val responseBody = tiltakskoordinatorsDeltaker.toResponse(
                harTilgangTilBruker,
                ulesteHendelserService.getUlesteHendelserForDeltaker(deltakerId),
            )

            call.respond(responseBody)
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

            val historikkResponse = historikk.toResponse(ansatte, arrangornavn, enheter, deltaker.deltakerliste.oppstart)
            val json = objectMapper.writePolymorphicListAsString(historikkResponse)
            call.respondText(json, ContentType.Application.Json)
        }
    }
}
