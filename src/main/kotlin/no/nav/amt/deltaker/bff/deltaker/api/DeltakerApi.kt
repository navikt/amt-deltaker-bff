package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreMalRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.GodkjenningAvNav
import no.nav.amt.deltaker.bff.deltaker.model.OppdatertDeltaker
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk.DeltakerEndringType
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
) {
    val log = LoggerFactory.getLogger(javaClass)

    authenticate("VEILEDER") {
        post("/deltaker") {
            val navIdent = getNavIdent()
            val pameldingRequest = call.receive<PameldingRequest>()
            val enhetsnummer = call.request.header("aktiv-enhet")
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), pameldingRequest.personident)
            val deltaker = deltakerService.opprettDeltaker(
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

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            deltakerService.opprettUtkast(
                opprinneligDeltaker = deltaker,
                utkast = OppdatertDeltaker(
                    mal = request.mal,
                    bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                    godkjentAvNav = null,
                ),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}/utenGodkjenning") {
            val navIdent = getNavIdent()
            val request = call.receive<PameldingUtenGodkjenningRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            deltakerService.meldPaUtenGodkjenning(
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
                ),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )

            call.respond(HttpStatusCode.OK)
        }

        delete("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltakerId = UUID.fromString(call.parameters["deltakerId"])
            val deltaker = deltakerService.get(deltakerId).getOrThrow()

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            if (deltaker.status.type != DeltakerStatus.Type.KLADD) {
                log.warn("Kan ikke slette deltaker med id $deltakerId som har status ${deltaker.status.type}")
                call.respond(HttpStatusCode.BadRequest, "Kan ikke slette deltaker")
            }
            deltakerService.slettKladd(deltakerId)

            log.info("$navIdent har slettet kladd for deltaker med id $deltakerId")

            call.respond(HttpStatusCode.OK)
        }

        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.BAKGRUNNSINFORMASJON,
                endring = DeltakerEndring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/mal") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreMalRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.MAL,
                endring = DeltakerEndring.EndreMal(request.mal),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/deltakelsesmengde") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreDeltakelsesmengdeRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.DELTAKELSESMENGDE,
                endring = DeltakerEndring.EndreDeltakelsesmengde(
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                ),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/startdato") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreStartdatoRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.STARTDATO,
                endring = DeltakerEndring.EndreStartdato(request.startdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        get("/deltaker/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på deltaker med id $deltaker")

            call.respond(deltaker.toDeltakerResponse())
        }
    }
}
