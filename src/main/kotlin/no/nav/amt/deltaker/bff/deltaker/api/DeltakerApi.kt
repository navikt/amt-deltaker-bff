package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.api.model.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
) {
    val log = LoggerFactory.getLogger(javaClass)

    fun komplettDeltakerResponse(deltaker: Deltaker): DeltakerResponse {
        val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
        val enhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }
        return deltaker.toDeltakerResponse(ansatte, enhet)
    }

    authenticate("VEILEDER") {
        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )

            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/innhold") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreInnholdRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreInnhold(finnValgtInnhold(request.innhold, deltaker)),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/deltakelsesmengde") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreDeltakelsesmengdeRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreDeltakelsesmengde(
                    deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                    dagerPerUke = request.dagerPerUke?.toFloat(),
                ),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/startdato") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreStartdatoRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreStartdato(request.startdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/sluttdato") {
            endringenErUtilgjengelig()
            return@post

            val navIdent = getNavIdent()
            val request = call.receive<EndreSluttdatoRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreSluttdato(request.sluttdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/sluttarsak") {
            endringenErUtilgjengelig()
            return@post

            val navIdent = getNavIdent()
            val request = call.receive<EndreSluttarsakRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreSluttarsak(request.aarsak),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/ikke-aktuell") {
            endringenErUtilgjengelig()
            return@post

            val navIdent = getNavIdent()
            val request = call.receive<IkkeAktuellRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.IkkeAktuell(request.aarsak),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/avslutt") {
            endringenErUtilgjengelig()
            return@post

            val navIdent = getNavIdent()
            val request = call.receive<AvsluttDeltakelseRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.AvsluttDeltakelse(request.aarsak, request.sluttdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        get("/deltaker/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på deltaker med id ${deltaker.id}")

            val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
            val navEnhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }

            call.respond(deltaker.toDeltakerResponse(ansatte, navEnhet))
        }

        get("/deltaker/{deltakerId}/historikk") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på historikk for deltaker med id ${deltaker.id}")

            val historikk = deltaker.getDeltakerHistorikSortert()

            val ansatte = navAnsattService.hentAnsatteForHistorikk(historikk)

            call.respond(historikk.toResponse(ansatte))
        }

        post("/deltaker/{deltakerId}/forleng") {
            endringenErUtilgjengelig()
            return@post

            val navIdent = getNavIdent()
            val request = call.receive<ForlengDeltakelseRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endring = DeltakerEndring.Endring.ForlengDeltakelse(request.sluttdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.endringenErUtilgjengelig() {
    call.respond(HttpStatusCode.ServiceUnavailable, "Endringen er midlertidig utilgjengelig, prøv igjen senere.")
}
