package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
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
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorsDeltaker
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerlisteResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.KoordinatorResponse
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.util.UUID

fun Routing.registerTiltakskoordinatorApi(
    deltakerService: DeltakerService,
    vurderingService: VurderingService,
    deltakerlisteService: DeltakerlisteService,
    tilgangskontrollService: TilgangskontrollService,
    tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
) {
    val apiPath = "/tiltakskoordinator/deltakerliste/{id}"

    fun lagTiltakskoordinatorsDeltaker(deltaker: Deltaker, navAnsattAzureId: UUID): TiltakskoordinatorsDeltaker {
        val harTilgang = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        return TiltakskoordinatorsDeltaker(deltaker, harTilgang, sisteVurdering)
    }

    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        get(apiPath) {
            val deltakerlisteId = getDeltakerlisteId()
            val deltakerliste = deltakerlisteService.hentMedFellesOppstart(deltakerlisteId).getOrThrow()
            val koordinatorer = tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)

            call.respond(deltakerliste.toResponse(koordinatorer))
        }

        get("$apiPath/deltakere") {
            val deltakerlisteId = getDeltakerlisteId()

            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerlisteId)
            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId)

            val navAnsattAzureId = call.getNavAnsattAzureId()

            val deltakere = deltakerService
                .getForDeltakerliste(deltakerlisteId)
                .filterNot { deltaker -> deltaker.skalSkjules() }
                .map { lagTiltakskoordinatorsDeltaker(it, navAnsattAzureId) }

            call.respond(deltakere.map { it.toDeltakerResponse() })
        }

        post("$apiPath/tilgang/legg-til") {
            val deltakerlisteId = getDeltakerlisteId()

            tilgangskontrollService.leggTilTiltakskoordinatorTilgang(call.getNavIdent(), deltakerlisteId).getOrThrow()

            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun RoutingContext.getDeltakerlisteId(): UUID {
    val id = call.parameters["id"] ?: throw IllegalArgumentException("Påkrevd URL parameter 'deltakerlisteId' mangler.")

    return try {
        UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("URL parameter 'deltakerlisteId' er ikke formattert riktig.")
    }
}

fun TiltakskoordinatorsDeltaker.toDeltakerResponse(): DeltakerResponse {
    val (fornavn, mellomnavn, etternavn) = visningsnavn()

    return DeltakerResponse(
        id = deltaker.id,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        status = DeltakerResponse.DeltakerStatusResponse(
            type = deltaker.status.type,
            aarsak = deltaker.status.aarsak?.let {
                DeltakerResponse.DeltakerStatusAarsakResponse(
                    it.type,
                )
            },
        ),
        vurdering = vurdering?.vurderingstype,
        beskyttelsesmarkering = beskyttelsesmarkering(),
    )
}

fun Deltakerliste.toResponse(koordinatorer: List<NavAnsatt>) = DeltakerlisteResponse(
    this.id,
    this.tiltak.tiltakskode,
    this.startDato,
    this.sluttDato,
    this.apentForPamelding,
    this.antallPlasser,
    koordinatorer.map { KoordinatorResponse(id = it.id, navn = it.navn) },
)

fun Deltaker.skalSkjules() = status.type in listOf(
    DeltakerStatus.Type.KLADD,
    DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
    DeltakerStatus.Type.AVBRUTT_UTKAST,
    DeltakerStatus.Type.FEILREGISTRERT,
    DeltakerStatus.Type.PABEGYNT_REGISTRERING,
)
