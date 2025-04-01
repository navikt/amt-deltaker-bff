package no.nav.amt.deltaker.bff.innbygger

import io.ktor.http.ContentType
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getPersonident
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.innbygger.model.InnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.innbygger.model.toInnbyggerDeltakerResponse
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

fun Routing.registerInnbyggerApi(
    deltakerService: DeltakerService,
    tilgangskontrollService: TilgangskontrollService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    innbyggerService: InnbyggerService,
    forslagService: ForslagService,
) {
    val scope = CoroutineScope(Dispatchers.IO)

    fun komplettInnbyggerDeltakerResponse(deltaker: Deltaker): InnbyggerDeltakerResponse {
        val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
        val enhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }
        val forslag = forslagService.getForDeltaker(deltaker.id)
        return deltaker.toInnbyggerDeltakerResponse(ansatte, enhet, forslag)
    }

    authenticate(AuthLevel.INNBYGGER.name) {
        get("/innbygger/{id}") {
            val innbygger = call.getPersonident()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["id"])).getOrThrow()
            tilgangskontrollService.verifiserInnbyggersTilgangTilDeltaker(innbygger, deltaker.navBruker.personident)

            scope.launch { deltakerService.oppdaterSistBesokt(deltaker) }

            call.respond(komplettInnbyggerDeltakerResponse(deltaker))
        }

        post("/innbygger/{id}/godkjenn-utkast") {
            val innbygger = call.getPersonident()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["id"])).getOrThrow()
            tilgangskontrollService.verifiserInnbyggersTilgangTilDeltaker(innbygger, deltaker.navBruker.personident)

            require(deltaker.status.type == DeltakerStatus.Type.UTKAST_TIL_PAMELDING) {
                "Deltaker ${deltaker.id} har ikke status ${DeltakerStatus.Type.UTKAST_TIL_PAMELDING}"
            }

            val oppdatertDeltaker = innbyggerService.godkjennUtkast(deltaker)

            MetricRegister.GODKJENT_UTKAST.inc()

            call.respond(komplettInnbyggerDeltakerResponse(oppdatertDeltaker))
        }

        get("/innbygger/{id}/historikk") {
            val innbygger = call.getPersonident()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["id"])).getOrThrow()
            tilgangskontrollService.verifiserInnbyggersTilgangTilDeltaker(innbygger, deltaker.navBruker.personident)

            val historikk = deltaker.getDeltakerHistorikkSortert()

            val ansatte = navAnsattService.hentAnsatteForHistorikk(historikk)
            val enheter = navEnhetService.hentEnheterForHistorikk(historikk)

            val arrangornavn = deltaker.deltakerliste.arrangor.getArrangorNavn()

            val json = objectMapper.writePolymorphicListAsString(historikk.toResponse(ansatte, arrangornavn, enheter))
            call.respondText(json, ContentType.Application.Json)
        }
    }
}
