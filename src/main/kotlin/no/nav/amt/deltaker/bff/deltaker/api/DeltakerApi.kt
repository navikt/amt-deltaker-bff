package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerHistorikkService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreMalRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    deltakerHistorikkService: DeltakerHistorikkService,
) {
    val log = LoggerFactory.getLogger(javaClass)

    authenticate("VEILEDER") {
        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.harSluttet()) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke endre bakgrunnsinformasjon for deltaker som har sluttet")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon),
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

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.harSluttet()) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke endre mål for deltaker som har sluttet")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.MAL,
                endring = DeltakerEndring.Endring.EndreMal(request.mal),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/deltakelsesmengde") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreDeltakelsesmengdeRequest>()
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.harSluttet()) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke endre deltakelsesmengde for deltaker som har sluttet")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.DELTAKELSESMENGDE,
                endring = DeltakerEndring.Endring.EndreDeltakelsesmengde(
                    deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                    dagerPerUke = request.dagerPerUke?.toFloat(),
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

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.harSluttet()) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke endre startdato for deltaker som har sluttet")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.STARTDATO,
                endring = DeltakerEndring.Endring.EndreStartdato(request.startdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/ikke-aktuell") {
            val navIdent = getNavIdent()
            val request = call.receive<IkkeAktuellRequest>()
            request.valider()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.harSluttet()) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke sette deltaker som har sluttet til IKKE AKTUELL")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.IKKE_AKTUELL,
                endring = DeltakerEndring.Endring.IkkeAktuell(request.aarsak),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        get("/deltaker/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på deltaker med id ${deltaker.id}")

            call.respond(deltaker.toDeltakerResponse())
        }

        get("/deltaker/{deltakerId}/historikk") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på historikk for deltaker med id ${deltaker.id}")

            val historikk = deltakerHistorikkService.getForDeltaker(deltaker.id)

            call.respond(historikk.toResponse())
        }

        post("/deltaker/{deltakerId}/forleng") {
            val navIdent = getNavIdent()
            val request = call.receive<ForlengDeltakelseRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            if (deltaker.sluttdato != null && deltaker.sluttdato.isAfter(request.sluttdato)) {
                call.respond(HttpStatusCode.BadRequest, "Ny sluttdato må være nyere enn opprinnelig sluttdato ved forlengelse")
                return@post
            }
            if (deltaker.deltakerliste.sluttDato?.isBefore(request.sluttdato) == true) {
                call.respond(HttpStatusCode.BadRequest, "Ny sluttdato kan ikke være senere enn deltakerlistens sluttdato ved forlengelse")
                return@post
            }
            if (deltaker.status.type != DeltakerStatus.Type.DELTAR && deltaker.status.type != DeltakerStatus.Type.HAR_SLUTTET) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke forlenge deltakelse for deltaker med status ${deltaker.status.type}")
                return@post
            } else if (deltaker.status.type == DeltakerStatus.Type.HAR_SLUTTET && deltaker.sluttdato?.isBefore(LocalDate.now().minusMonths(2)) == true) {
                call.respond(HttpStatusCode.BadRequest, "Kan ikke forlenge deltakelse for deltaker som sluttet for mer enn to måneder siden")
                return@post
            }

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.FORLENGELSE,
                endring = DeltakerEndring.Endring.ForlengDeltakelse(request.sluttdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }
    }
}
