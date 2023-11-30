package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreMalRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForslagRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
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
            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), pameldingRequest.personident)
            val deltaker = deltakerService.opprettDeltaker(
                deltakerlisteId = pameldingRequest.deltakerlisteId,
                personident = pameldingRequest.personident,
                opprettetAv = navIdent,
            )
            call.respond(deltaker)
        }

        post("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val request = call.receive<ForslagRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"]))

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            deltakerService.opprettForslag(
                opprinneligDeltaker = deltaker,
                forslag = OppdatertDeltaker(
                    mal = request.mal,
                    bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                    deltakelsesprosent = request.deltakelsesprosent,
                    dagerPerUke = request.dagerPerUke,
                    godkjentAvNav = null,
                ),
                endretAv = navIdent,
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}/utenGodkjenning") {
            val navIdent = getNavIdent()
            val request = call.receive<PameldingUtenGodkjenningRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"]))

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
                    ),
                ),
                endretAv = navIdent,
            )

            call.respond(HttpStatusCode.OK)
        }

        delete("/pamelding/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltakerId = UUID.fromString(call.parameters["deltakerId"])
            val deltaker = deltakerService.get(deltakerId)

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            if (deltaker.status.type != DeltakerStatus.Type.UTKAST) {
                log.warn("Kan ikke slette deltaker med id $deltakerId som har status ${deltaker.status.type}")
                call.respond(HttpStatusCode.BadRequest, "Kan ikke slette deltaker")
            }
            deltakerService.slettUtkast(deltakerId)

            log.info("$navIdent har slettet utkast for deltaker med id $deltakerId")

            call.respond(HttpStatusCode.OK)
        }

        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"]))

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.BAKGRUNNSINFORMASJON,
                endring = DeltakerEndring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon),
                endretAv = navIdent,
            )

            call.respond(oppdatertDeltaker)
        }

        post("/deltaker/{deltakerId}/mal") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreMalRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"]))

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.personident)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringType = DeltakerEndringType.MAL,
                endring = DeltakerEndring.EndreMal(request.mal),
                endretAv = navIdent,
            )

            call.respond(oppdatertDeltaker)
        }
    }
}
