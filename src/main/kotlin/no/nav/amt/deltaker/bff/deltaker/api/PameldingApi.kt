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
import no.nav.amt.deltaker.bff.deltaker.api.model.KladdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.model.GodkjenningAvNav
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
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
            val request = call.receive<PameldingRequest>()

            val enhetsnummer = call.request.header("aktiv-enhet")
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), request.personident)
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = request.deltakerlisteId,
                personident = request.personident,
                opprettetAv = navIdent,
                opprettetAvEnhet = enhetsnummer,
            )
            call.respond(deltaker.toDeltakerResponse())
        }

        post("/pamelding/{deltakerId}/kladd") {
            val navIdent = getNavIdent()
            val request = call.receive<KladdRequest>()
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.upsertKladd(
                kladd = Kladd(
                    opprinneligDeltaker = deltaker,
                    pamelding = Pamelding(
                        mal = request.mal,
                        bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                        deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                        dagerPerUke = request.dagerPerUke?.toFloat(),
                        endretAv = navIdent,
                        endretAvEnhet = enhetsnummer,
                    ),
                ),
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val request = call.receive<UtkastRequest>()
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.upsertUtkast(
                Utkast(
                    opprinneligDeltaker = deltaker,
                    pamelding = Pamelding(
                        mal = request.mal,
                        bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                        deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                        dagerPerUke = request.dagerPerUke?.toFloat(),
                        endretAv = navIdent,
                        endretAvEnhet = enhetsnummer,
                    ),
                    godkjentAvNav = null,
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
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.meldPaUtenGodkjenning(
                Utkast(
                    opprinneligDeltaker = deltaker,
                    pamelding = Pamelding(
                        mal = request.mal,
                        bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                        deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                        dagerPerUke = request.dagerPerUke?.toFloat(),
                        endretAv = navIdent,
                        endretAvEnhet = enhetsnummer,
                    ),
                    godkjentAvNav = GodkjenningAvNav(
                        type = request.begrunnelse.type,
                        beskrivelse = request.begrunnelse.beskrivelse,
                        godkjentAv = navIdent,
                        godkjentAvEnhet = enhetsnummer,
                    ),
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
