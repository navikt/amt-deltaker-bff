package no.nav.amt.deltaker.bff.tiltakskoordinator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.application.plugins.getNavAnsattAzureId
import no.nav.amt.deltaker.bff.application.plugins.getNavIdent
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusAarsakResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerStatusResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.response.DeltakerlisteResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.Tiltakskoordinator
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.UlestHendelseType
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import java.util.UUID

fun Routing.registerTiltakskoordinatorDeltakerlisteApi(
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    tiltakskoordinatorService: TiltakskoordinatorService,
    tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
    navAnsattService: NavAnsattService,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        get(apiPath) {
            val deltakerlisteId = getDeltakerlisteId()
            val deltakerliste = deltakerlisteService.hentMedFellesOppstart(deltakerlisteId).getOrThrow()

            val paaloggetNavAnsatt = navAnsattService.hentNavAnsatt(call.getNavIdent())

            val koordinatorer = tiltakskoordinatorTilgangRepository.hentKoordinatorer(
                deltakerlisteId = deltakerlisteId,
                paaloggetNavAnsattId = paaloggetNavAnsatt.id,
            )

            call.respond(deltakerliste.toResponse(koordinatorer))
        }

        get("$apiPath/deltakere") {
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val deltakerlisteId = getDeltakerlisteId()
            val navIdent = call.getNavIdent()

            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navIdent, deltakerlisteId)

            val deltakere = tiltakskoordinatorService
                .hentDeltakereForDeltakerliste(deltakerlisteId)
                .toDeltakerResponses(tilgangskontrollService, navAnsattAzureId)

            call.respond(deltakere)
        }

        post("$apiPath/deltakere/tildel-plass") {
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            val deltakerIder = call.receive<List<UUID>>()
            val deltakerlisteId = getDeltakerlisteId()

            tilgangskontrollService.tilgangTilDeltakereGuard(deltakerIder, deltakerlisteId, navIdent)

            val oppdaterteDeltakere = tiltakskoordinatorService.endreDeltakere(
                deltakerIder,
                EndringFraTiltakskoordinator.TildelPlass,
                navIdent,
            )
            val deltakereResponse = oppdaterteDeltakere.toDeltakerResponses(tilgangskontrollService, navAnsattAzureId)

            call.respond(deltakereResponse)
        }

        post("$apiPath/deltakere/sett-paa-venteliste") {
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            val deltakerIder = call.receive<List<UUID>>()
            val deltakerlisteId = getDeltakerlisteId()

            tilgangskontrollService.tilgangTilDeltakereGuard(deltakerIder, deltakerlisteId, navIdent)

            val oppdaterteDeltakere = tiltakskoordinatorService.endreDeltakere(
                deltakerIder,
                EndringFraTiltakskoordinator.SettPaaVenteliste,
                navIdent,
            )
            val deltakereResponse = oppdaterteDeltakere.toDeltakerResponses(tilgangskontrollService, navAnsattAzureId)

            call.respond(deltakereResponse)
        }

        post("$apiPath/deltakere/del-med-arrangor") {
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            val deltakerlisteId = getDeltakerlisteId()
            val deltakerIder = call.receive<List<UUID>>()

            tilgangskontrollService.tilgangTilDeltakereGuard(deltakerIder, deltakerlisteId, navIdent)

            val oppdaterteDeltakere = tiltakskoordinatorService
                .endreDeltakere(
                    deltakerIder,
                    EndringFraTiltakskoordinator.DelMedArrangor,
                    navIdent,
                ).toDeltakerResponses(tilgangskontrollService, navAnsattAzureId)

            call.respond(oppdaterteDeltakere)
        }

        post("$apiPath/deltakere/gi-avslag") {
            val navAnsattAzureId = call.getNavAnsattAzureId()
            val navIdent = call.getNavIdent()
            val deltakerlisteId = getDeltakerlisteId()
            val request = call.receive<AvslagRequest>()

            tilgangskontrollService.tilgangTilDeltakereGuard(listOf(request.deltakerId), deltakerlisteId, navIdent)

            val oppdatertDeltaker = tiltakskoordinatorService.giAvslag(request, navIdent)

            val harTilgang = tilgangskontrollService.harKoordinatorTilgangTilPerson(navAnsattAzureId, oppdatertDeltaker.navBruker)

            call.respond(oppdatertDeltaker.toDeltakerResponse(harTilgang))
        }

        post("$apiPath/tilgang/legg-til") {
            tilgangskontrollService
                .leggTilTiltakskoordinatorTilgang(
                    navIdent = call.getNavIdent(),
                    deltakerlisteId = getDeltakerlisteId(),
                ).getOrThrow()

            call.respond(HttpStatusCode.OK)
        }

        post("$apiPath/tilgang/fjern") {
            tilgangskontrollService
                .fjernTiltakskoordinatorTilgang(
                    call.getNavIdent(),
                    getDeltakerlisteId(),
                ).getOrThrow()

            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun List<TiltakskoordinatorsDeltaker>.toDeltakerResponses(
    tilgangskontrollService: TilgangskontrollService,
    navAnsattAzureId: UUID,
) = map { deltaker ->
    val harTilgangTilAASeNavn =
        tilgangskontrollService.harKoordinatorTilgangTilPerson(navAnsattAzureId, deltaker.navBruker)

    deltaker.toDeltakerResponse(harTilgangTilAASeNavn)
}

fun TiltakskoordinatorsDeltaker.toDeltakerResponse(harTilgang: Boolean): DeltakerResponse {
    val (fornavn, mellomnavn, etternavn) = navBruker.getVisningsnavn(harTilgang)

    return DeltakerResponse(
        id = id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        status = DeltakerStatusResponse(
            type = status.type,
            aarsak = status.aarsak?.let {
                DeltakerStatusAarsakResponse(
                    it.type,
                    it.beskrivelse,
                )
            },
        ),
        vurdering = vurdering?.vurderingstype,
        beskyttelsesmarkering = beskyttelsesmarkering,
        navEnhet = navEnhet,
        erManueltDeltMedArrangor = erManueltDeltMedArrangor,
        feilkode = feilkode,
        ikkeDigitalOgManglerAdresse = ikkeDigitalOgManglerAdresse,
        harAktiveForslag = forslag.any { f -> f.status == Forslag.Status.VenterPaSvar },
        erNyDeltaker = ulesteHendelser.any {
            it.hendelse is UlestHendelseType.InnbyggerGodkjennUtkast ||
                it.hendelse is UlestHendelseType.NavGodkjennUtkast
        },
        harOppdateringFraNav = ulesteHendelser.any {
            it.hendelse is UlestHendelseType.IkkeAktuell ||
                it.hendelse is UlestHendelseType.AvsluttDeltakelse ||
                it.hendelse is UlestHendelseType.AvbrytDeltakelse ||
                it.hendelse is UlestHendelseType.ReaktiverDeltakelse
        },
        kanEndres = kanEndres,
    )
}

data class AvslagRequest(
    val deltakerId: UUID,
    val aarsak: EndringFraTiltakskoordinator.Avslag.Aarsak,
    val begrunnelse: String?,
)

fun RoutingContext.getDeltakerlisteId(): UUID {
    val id =
        call.parameters["id"] ?: throw IllegalArgumentException("Påkrevd URL parameter 'deltakerlisteId' mangler.")

    return try {
        UUID.fromString(id)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("URL parameter 'deltakerlisteId' er ikke formattert riktig.")
    }
}

fun Deltakerliste.toResponse(koordinatorer: List<Tiltakskoordinator>) = DeltakerlisteResponse(
    id = this.id,
    navn = this.navn,
    tiltakskode = this.tiltak.tiltakskode,
    startdato = this.startDato,
    sluttdato = this.sluttDato,
    oppstartstype = this.getOppstartstype(),
    apentForPamelding = this.apentForPamelding,
    antallPlasser = this.antallPlasser,
    koordinatorer = koordinatorer,
)
