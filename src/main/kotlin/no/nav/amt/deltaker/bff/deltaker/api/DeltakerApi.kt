package no.nav.amt.deltaker.bff.deltaker.api

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
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    deltakerHistorikkService: DeltakerHistorikkService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
) {
    val log = LoggerFactory.getLogger(javaClass)

    authenticate("VEILEDER") {
        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(oppdatertDeltaker.toDeltakerResponse())
        }

        post("/deltaker/{deltakerId}/innhold") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreInnholdRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                opprinneligDeltaker = deltaker,
                endringstype = DeltakerEndring.Endringstype.INNHOLD,
                endring = DeltakerEndring.Endring.EndreInnhold(finnValgtInnhold(request.innhold, deltaker)),
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

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

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

            request.valider(deltaker)

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

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

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

            val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
            val navEnhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }

            call.respond(deltaker.toDeltakerResponse(ansatte, navEnhet))
        }

        get("/deltaker/{deltakerId}/historikk") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på historikk for deltaker med id ${deltaker.id}")

            val historikk = deltakerHistorikkService.getForDeltaker(deltaker.id)

            val ansatte = navAnsattService.hentAnsatteForHistorikk(historikk)

            call.respond(historikk.toResponse(ansatte))
        }

        post("/deltaker/{deltakerId}/forleng") {
            val navIdent = getNavIdent()
            val request = call.receive<ForlengDeltakelseRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.header("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

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
