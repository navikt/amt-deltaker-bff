package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.metrics.MetricRegister
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.KladdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.PameldingUtenGodkjenningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltakelsesinnhold
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Kladd
import no.nav.amt.deltaker.bff.deltaker.model.Pamelding
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerPameldingApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    pameldingService: PameldingService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    forslagService: ForslagService,
    amtDistribusjonClient: AmtDistribusjonClient,
) {
    val log = LoggerFactory.getLogger(javaClass)

    suspend fun komplettDeltakerResponse(deltaker: Deltaker): DeltakerResponse {
        val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
        val enhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }
        val digitalBruker = amtDistribusjonClient.digitalBruker(deltaker.navBruker.personident)
        val forslag = forslagService.getForDeltaker(deltaker.id)

        return deltaker.toDeltakerResponse(ansatte, enhet, digitalBruker, forslag)
    }

    authenticate("VEILEDER") {
        post("/pamelding") {
            val request = call.receive<PameldingRequest>()

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), request.personident)
            val deltaker = pameldingService.opprettKladd(
                deltakerlisteId = request.deltakerlisteId,
                personident = request.personident,
            )

            call.respond(komplettDeltakerResponse(deltaker))
        }

        post("/pamelding/{deltakerId}/kladd") {
            val navIdent = getNavIdent()
            val request = call.receive<KladdRequest>().sanitize()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            request.valider(deltaker)

            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.upsertKladd(
                kladd = Kladd(
                    opprinneligDeltaker = deltaker,
                    pamelding = Pamelding(
                        deltakelsesinnhold = Deltakelsesinnhold(
                            deltaker.deltakelsesinnhold?.ledetekst,
                            finnValgtInnhold(request.innhold, deltaker),
                        ),
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

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val digitalBruker = amtDistribusjonClient.digitalBruker(deltaker.navBruker.personident)
            request.valider(deltaker, digitalBruker)

            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            val oppdatertDeltaker = pameldingService.upsertUtkast(
                Utkast(
                    deltakerId = deltaker.id,
                    pamelding = Pamelding(
                        deltakelsesinnhold = Deltakelsesinnhold(
                            deltaker.deltakelsesinnhold?.ledetekst,
                            finnValgtInnhold(request.innhold, deltaker),
                        ),
                        bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                        deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                        dagerPerUke = request.dagerPerUke?.toFloat(),
                        endretAv = navIdent,
                        endretAvEnhet = enhetsnummer,
                    ),
                    godkjentAvNav = false,
                ),
            )

            MetricRegister.DELT_UTKAST.inc()

            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/pamelding/{deltakerId}/avbryt") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.avbrytUtkast(
                deltakerId = deltaker.id,
                avbruttAv = navIdent,
                avbruttAvEnhet = enhetsnummer,
            )

            call.respond(HttpStatusCode.OK)
        }

        post("/pamelding/{deltakerId}/utenGodkjenning") {
            val navIdent = getNavIdent()
            val request = call.receive<PameldingUtenGodkjenningRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            request.valider(deltaker)

            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            pameldingService.upsertUtkast(
                Utkast(
                    deltakerId = deltaker.id,
                    pamelding = Pamelding(
                        deltakelsesinnhold = Deltakelsesinnhold(
                            deltaker.deltakerliste.tiltak.innhold?.ledetekst,
                            finnValgtInnhold(request.innhold, deltaker),
                        ),
                        bakgrunnsinformasjon = request.bakgrunnsinformasjon,
                        deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                        dagerPerUke = request.dagerPerUke?.toFloat(),
                        endretAv = navIdent,
                        endretAvEnhet = enhetsnummer,
                    ),
                    godkjentAvNav = true,
                ),
            )

            MetricRegister.PAMELDT_UTEN_UTKAST.inc()

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

fun ApplicationRequest.headerNotNull(navn: String): String {
    val header = call.request.header(navn)
    require(header != null) { "Påkrevd header: $navn er null" }
    return header
}
