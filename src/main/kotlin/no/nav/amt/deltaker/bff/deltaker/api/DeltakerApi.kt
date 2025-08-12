package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import jakarta.ws.rs.ForbiddenException
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.application.plugins.writePolymorphicListAsString
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.model.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.AvvisForslagRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.DeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreAvslutningRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreBakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreDeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreInnholdRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreSluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndreStartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.EndringsforslagRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.Endringsrequest
import no.nav.amt.deltaker.bff.deltaker.api.model.FjernOppstartsdatoRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.ReaktiverDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.finnValgtInnhold
import no.nav.amt.deltaker.bff.deltaker.api.model.getArrangorNavn
import no.nav.amt.deltaker.bff.deltaker.api.model.toDeltakerResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toResponse
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
    navAnsattService: NavAnsattService,
    navEnhetService: NavEnhetService,
    forslagService: ForslagService,
    amtDistribusjonClient: AmtDistribusjonClient,
    sporbarhetsloggService: SporbarhetsloggService,
    unleashToggle: UnleashToggle,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun komplettDeltakerResponse(deltaker: Deltaker): DeltakerResponse {
        val ansatte = navAnsattService.hentAnsatteForDeltaker(deltaker)
        val enhet = deltaker.vedtaksinformasjon?.sistEndretAvEnhet?.let { navEnhetService.hentEnhet(it) }
        val digitalBruker = amtDistribusjonClient.digitalBruker(deltaker.navBruker.personident)
        val forslag = forslagService.getForDeltaker(deltaker.id)
        return deltaker.toDeltakerResponse(ansatte, enhet, digitalBruker, forslag)
    }

    fun illegalUpdateGuard(deltaker: Deltaker, tillatEndringUtenOppfPeriode: Boolean) {
        if (!deltaker.kanEndres) {
            log.error("Kan ikke endre deltaker med id ${deltaker.id} som er låst")
            throw ForbiddenException("Kan ikke endre låst deltaker ${deltaker.id}")
        }

        if (deltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT) {
            throw ForbiddenException("Kan ikke endre låst deltaker ${deltaker.id}")
        }

        if (!unleashToggle.erKometMasterForTiltakstype(deltaker.deltakerliste.tiltak.arenaKode)) {
            throw ForbiddenException("Kan ikke utføre endring på deltaker ${deltaker.id}")
        }

        if (!deltaker.navBruker.harAktivOppfolgingsperiode && !tillatEndringUtenOppfPeriode) {
            log.warn("Kan ikke endre deltaker med id ${deltaker.id} som ikke har aktiv oppfølgingsperiode")
            throw IllegalArgumentException("Kan ikke endre deltaker som ikke har aktiv oppfølgingsperiode")
        }
    }

    suspend fun handleEndring(
        call: ApplicationCall,
        request: Endringsrequest,
        endring: (deltaker: Deltaker) -> DeltakerEndring.Endring,
    ) {
        val navIdent = call.getNavIdent()
        val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
        val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

        tilgangskontrollService.verifiserSkrivetilgang(call.getNavAnsattAzureId(), deltaker.navBruker.personident)
        illegalUpdateGuard(deltaker, request.tillattEndringUtenAktivOppfolgingsperiode())

        request.valider(deltaker)

        val forslag = if (request is EndringsforslagRequest) {
            request.forslagId?.let { forslagService.get(it).getOrThrow() }
        } else {
            null
        }

        val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
            deltaker = deltaker,
            endring = endring(deltaker),
            endretAv = navIdent,
            endretAvEnhet = enhetsnummer,
            forslagId = forslag?.id,
        )

        forslag?.let { forslagService.delete(it.id) }

        call.respond(komplettDeltakerResponse(oppdatertDeltaker))
    }

    authenticate(AuthLevel.VEILEDER.name) {
        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.EndreBakgrunnsinformasjon(request.bakgrunnsinformasjon)
            }
        }

        post("/deltaker/{deltakerId}/innhold") {
            val request = call.receive<EndreInnholdRequest>()
            handleEndring(call, request) { deltaker ->
                DeltakerEndring.Endring.EndreInnhold(
                    deltaker.deltakerliste.tiltak.innhold
                        ?.ledetekst,
                    finnValgtInnhold(request.innhold, deltaker),
                )
            }
        }

        post("/deltaker/{deltakerId}/deltakelsesmengde") {
            val request = call.receive<EndreDeltakelsesmengdeRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.EndreDeltakelsesmengde(
                    deltakelsesprosent = request.deltakelsesprosent?.toFloat(),
                    dagerPerUke = request.dagerPerUke?.toFloat(),
                    begrunnelse = request.begrunnelse,
                    gyldigFra = request.gyldigFra,
                )
            }
        }

        post("/deltaker/{deltakerId}/startdato") {
            val request = call.receive<EndreStartdatoRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.EndreStartdato(
                    startdato = request.startdato,
                    sluttdato = request.sluttdato,
                    begrunnelse = request.begrunnelse,
                )
            }
        }

        post("/deltaker/{deltakerId}/sluttdato") {
            val request = call.receive<EndreSluttdatoRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.EndreSluttdato(request.sluttdato, request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}/sluttarsak") {
            val request = call.receive<EndreSluttarsakRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.EndreSluttarsak(request.aarsak, request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}/ikke-aktuell") {
            val request = call.receive<IkkeAktuellRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.IkkeAktuell(request.aarsak, request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}/reaktiver") {
            val request = call.receive<ReaktiverDeltakelseRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.ReaktiverDeltakelse(LocalDate.now(), request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}/avslutt") {
            val request = call.receive<AvsluttDeltakelseRequest>()
            handleEndring(call, request) {
                if (request.harDeltatt() && request.harFullfort()) {
                    require(request.sluttdato != null) { "Sluttdato er påkrevd for å avslutte deltakelse" }
                    DeltakerEndring.Endring.AvsluttDeltakelse(request.aarsak, request.sluttdato, request.begrunnelse)
                } else if (request.harDeltatt() && !request.harFullfort()) {
                    require(request.aarsak != null) { "Årsak er påkrevd for å avbryte deltakelse" }
                    require(request.sluttdato != null) { "Sluttdato er påkrevd for å avbryte deltakelse" }
                    DeltakerEndring.Endring.AvbrytDeltakelse(request.aarsak, request.sluttdato, request.begrunnelse)
                } else {
                    require(request.aarsak != null) { "Årsak er påkrevd for å sette deltaker til ikke aktuell" }
                    DeltakerEndring.Endring.IkkeAktuell(request.aarsak, request.begrunnelse)
                }
            }
        }

        post("/deltaker/{deltakerId}/endre-avslutning") {
            val request = call.receive<EndreAvslutningRequest>()
            handleEndring(call, request) {
                if (request.harDeltatt() && request.harFullfort()) {
                    DeltakerEndring.Endring.EndreAvslutning(request.aarsak, true, request.begrunnelse)
                } else if (request.harDeltatt() && !request.harFullfort()) {
                    require(request.aarsak != null) { "Årsak er påkrevd for å avbryte deltakelse" }
                    DeltakerEndring.Endring.EndreAvslutning(request.aarsak, false, request.begrunnelse)
                } else {
                    require(request.aarsak != null) { "Årsak er påkrevd for å sette deltaker til ikke aktuell" }
                    DeltakerEndring.Endring.IkkeAktuell(request.aarsak, request.begrunnelse)
                }
            }
        }

        post("/deltaker/{deltakerId}/forleng") {
            val request = call.receive<ForlengDeltakelseRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.ForlengDeltakelse(request.sluttdato, request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}/fjern-oppstartsdato") {
            val request = call.receive<FjernOppstartsdatoRequest>()
            handleEndring(call, request) {
                DeltakerEndring.Endring.FjernOppstartsdato(request.begrunnelse)
            }
        }

        post("/deltaker/{deltakerId}") {
            val request = call.receive<DeltakerRequest>()
            val deltakerId = call.parameters["deltakerId"]
            val navIdent = call.getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(deltakerId)).getOrThrow()

            if (request.personident != deltaker.navBruker.personident) {
                log.warn("$deltakerId ble forsøkt lest med en annen navbruker i kontekst.")
                call.respond(HttpStatusCode.BadRequest)
            }

            tilgangskontrollService.verifiserLesetilgang(call.getNavAnsattAzureId(), deltaker.navBruker.personident)
            sporbarhetsloggService.sendAuditLog(navIdent = navIdent, deltakerPersonIdent = deltaker.navBruker.personident)

            call.respond(komplettDeltakerResponse(deltaker))
        }

        get("/deltaker/{deltakerId}/historikk") {
            val navIdent = call.getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(call.getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på historikk for deltaker med id ${deltaker.id}")

            val historikk = deltaker.getDeltakerHistorikkForVisning()

            val ansatte = navAnsattService.hentAnsatteForHistorikk(historikk)
            val enheter = navEnhetService.hentEnheterForHistorikk(historikk)

            val arrangornavn = deltaker.deltakerliste.arrangor.getArrangorNavn()
            val historikkResponse = historikk.toResponse(ansatte, arrangornavn, enheter, deltaker.deltakerliste.oppstart)
            val json = objectMapper.writePolymorphicListAsString(historikkResponse)
            call.respondText(json, ContentType.Application.Json)
        }

        post("/forslag/{forslagId}/avvis") {
            val navIdent = call.getNavIdent()
            val request = call.receive<AvvisForslagRequest>()
            val forslag = forslagService.get(UUID.fromString(call.parameters["forslagId"])).getOrThrow()
            val deltaker = deltakerService.get(forslag.deltakerId).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(call.getNavAnsattAzureId(), deltaker.navBruker.personident)

            forslagService.avvisForslag(
                opprinneligForslag = forslag,
                begrunnelse = request.begrunnelse,
                avvistAvAnsatt = navIdent,
                avvistAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(deltaker))
        }
    }
}
