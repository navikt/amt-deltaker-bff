package no.nav.amt.deltaker.bff.deltaker.api

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.api.model.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.api.model.AvvisForslagRequest
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
import no.nav.amt.deltaker.bff.deltaker.api.utils.validerDeltakerKanReaktiveres
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.lib.models.arrangor.melding.Forslag
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Routing.registerDeltakerApi(
    tilgangskontrollService: TilgangskontrollService,
    deltakerService: DeltakerService,
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
        post("/deltaker/{deltakerId}/bakgrunnsinformasjon") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreBakgrunnsinformasjonRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
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
                deltaker = deltaker,
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
                deltaker = deltaker,
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
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreStartdato(
                    startdato = request.startdato,
                    sluttdato = request.sluttdato,
                ),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/sluttdato") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreSluttdatoRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreSluttdato(request.sluttdato),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/sluttarsak") {
            val navIdent = getNavIdent()
            val request = call.receive<EndreSluttarsakRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.EndreSluttarsak(request.aarsak),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/ikke-aktuell") {
            val navIdent = getNavIdent()
            val request = call.receive<IkkeAktuellRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.IkkeAktuell(request.aarsak),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/reaktiver") {
            val navIdent = getNavIdent()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            validerDeltakerKanReaktiveres(deltaker)

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.ReaktiverDeltakelse(LocalDate.now()),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/avslutt") {
            val navIdent = getNavIdent()
            val request = call.receive<AvsluttDeltakelseRequest>()

            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)

            val oppdatertDeltaker = if (request.harDeltatt() && request.sluttdato != null) {
                deltakerService.oppdaterDeltaker(
                    deltaker = deltaker,
                    endring = DeltakerEndring.Endring.AvsluttDeltakelse(request.aarsak, request.sluttdato),
                    endretAv = navIdent,
                    endretAvEnhet = enhetsnummer,
                )
            } else {
                deltakerService.oppdaterDeltaker(
                    deltaker = deltaker,
                    endring = DeltakerEndring.Endring.IkkeAktuell(request.aarsak),
                    endretAv = navIdent,
                    endretAvEnhet = enhetsnummer,
                )
            }
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        post("/deltaker/{deltakerId}/forleng") {
            val navIdent = getNavIdent()
            val request = call.receive<ForlengDeltakelseRequest>()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            request.valider(deltaker)
            val forslag = request.forslagId?.let { forslagService.get(it).getOrThrow() }

            val godkjentForslag = forslag?.copy(
                status = Forslag.Status.Godkjent(
                    godkjentAv = Forslag.NavAnsatt(
                        id = navAnsattService.hentEllerOpprettNavAnsatt(navIdent).id,
                        enhetId = navEnhetService.hentOpprettEllerOppdaterNavEnhet(enhetsnummer).id,
                    ),
                    godkjent = LocalDateTime.now(),
                ),
            )

            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker = deltaker,
                endring = DeltakerEndring.Endring.ForlengDeltakelse(request.sluttdato, request.begrunnelse, godkjentForslag),
                endretAv = navIdent,
                endretAvEnhet = enhetsnummer,
                forslagId = request.forslagId,
            )
            call.respond(komplettDeltakerResponse(oppdatertDeltaker))
        }

        get("/deltaker/{deltakerId}") {
            val navIdent = getNavIdent()
            val deltaker = deltakerService.get(UUID.fromString(call.parameters["deltakerId"])).getOrThrow()
            tilgangskontrollService.verifiserLesetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)
            log.info("NAV-ident $navIdent har gjort oppslag på deltaker med id ${deltaker.id}")

            call.respond(komplettDeltakerResponse(deltaker))
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

        post("/forslag/{forslagId}/avvis") {
            val navIdent = getNavIdent()
            val request = call.receive<AvvisForslagRequest>()
            val forslag = forslagService.get(UUID.fromString(call.parameters["forslagId"])).getOrThrow()
            val deltaker = deltakerService.get(forslag.deltakerId).getOrThrow()
            val enhetsnummer = call.request.headerNotNull("aktiv-enhet")

            tilgangskontrollService.verifiserSkrivetilgang(getNavAnsattAzureId(), deltaker.navBruker.personident)

            forslagService.avvisForslag(
                opprinneligForslag = forslag,
                begrunnelse = request.begrunnelseFraNav,
                avvistAvAnsatt = navIdent,
                avvistAvEnhet = enhetsnummer,
            )
            call.respond(komplettDeltakerResponse(deltaker))
        }
    }
}
