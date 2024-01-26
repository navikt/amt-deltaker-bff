package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.api.model.AvbrytUtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.model.GodkjenningAvNav
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerPameldingApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    pameldingService: PameldingService,
) {
    val log = LoggerFactory.getLogger(javaClass)

    authenticate("VEILEDER") {
        post("/pamelding") {
            val navIdent = getNavIdent()
            val pameldingRequest = call.receive<PameldingRequest>()
            val enhetsnummer = call.request.header("aktiv-enhet")
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), pameldingRequest.personident)
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = pameldingRequest.deltakerlisteId,
                personident = pameldingRequest.personident,
                opprettetAv = navIdent,
                opprettetAvEnhet = enhetsnummer,
            )
            call.respond(deltaker.toDeltakerResponse())
        }

        // Deprecated bør byttes til /pamelding
        post("/deltaker") {
            val navIdent = getNavIdent()
            val pameldingRequest = call.receive<PameldingRequest>()
            val enhetsnummer = call.request.header("aktiv-enhet")
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), pameldingRequest.personident)
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = pameldingRequest.deltakerlisteId,
                personident = pameldingRequest.personident,
                opprettetAv = navIdent,
                opprettetAvEnhet = enhetsnummer,
            )
            call.respond(deltaker.toDeltakerResponse())
        }

        post("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val request = call.receive<UtkastRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.opprettUtkast(
                opprinneligDeltaker = deltaker,
                utkast = OppdatertDeltaker(
                    mal = request.mal,
                    bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                    godkjentAvNav = null,
                    endretAv = navIdent,
                    endretAvEnhet = enhetsnummer,
                ),
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}/avbryt") {
            val navIdent = getNavIdent()
            val request = call.receive<AvbrytUtkastRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.avbrytUtkast(
                opprinneligDeltaker = deltaker,
                navIdent = navIdent,
                enhetsnummer = enhetsnummer,
                aarsak = request.aarsak,
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}/utenGodkjenning") {
            val navIdent = getNavIdent()
            val request = call.receive<PameldingUtenGodkjenningRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.meldPaUtenGodkjenning(
                opprinneligDeltaker = deltaker,
                oppdatertDeltaker = OppdatertDeltaker(
                    mal = request.mal,
                    bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                    godkjentAvNav = GodkjenningAvNav(
                        type = request.begrunnelse.type,
                        beskrivelse = request.begrunnelse.beskrivelse,
                        godkjentAv = navIdent,
                        godkjentAvEnhet = enhetsnummer,
                    ),
                    endretAv = navIdent,
                    endretAvEnhet = enhetsnummer,
                ),
            )

            call.respond(HttpStatusCode.OK)
        }

        delete("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltakerId = UUID.fromString(call.parameters["deltakerId"])
            val deltaker = deltakerService.get(deltakerId).getOrThrow()

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (!pameldingService.slettKladd(deltaker)) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke slette deltaker")
            }

            log.info("$navIdent har slettet kladd for deltaker med id $deltakerId")

            call.respond(HttpStatusCode.OK)
        }
    }
}
