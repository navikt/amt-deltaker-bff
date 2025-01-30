package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.AuthLevel
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerResponse
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.DeltakerlisteResponse
import java.util.UUID

fun Routing.registerTiltakskoordinatorApi(deltakerService: DeltakerService, deltakerlisteRepository: DeltakerlisteRepository) {
    authenticate(AuthLevel.TILTAKSKOORDINATOR.name) {
        if (!Environment.isProd()) {
            get("/tiltakskoordinator/deltakerliste/{id}") {
                val deltakerlisteId = UUID.fromString(call.parameters["id"])
                val deltakerliste = deltakerlisteRepository.get(deltakerlisteId).getOrThrow()

                call.respond(deltakerliste.toResponse())
            }

            get("/tiltakskoordinator/deltakerliste/{id}/deltakere") {
                val deltakerlisteId = UUID.fromString(call.parameters["id"])
                val deltakere = deltakerService.getForDeltakerliste(deltakerlisteId)

                call.respond(deltakere.map { it.toDeltakerResponse() })
            }
        }
    }
}

fun Deltaker.toDeltakerResponse() = DeltakerResponse(
    id = this.id,
    fornavn = if (navBruker.erAdressebeskyttet) "" else this.navBruker.fornavn,
    mellomnavn = if (navBruker.erAdressebeskyttet) null else this.navBruker.mellomnavn,
    etternavn = if (navBruker.erAdressebeskyttet) "" else this.navBruker.etternavn,
    DeltakerResponse.DeltakerStatusResponse(
        type = this.status.type,
        aarsak = this.status.aarsak?.let {
            DeltakerResponse.DeltakerStatusAarsakResponse(
                it.type,
            )
        },
    ),
)

fun Deltakerliste.toResponse() = DeltakerlisteResponse(
    this.id,
    this.startDato,
    this.sluttDato,
    this.apentForPamelding,
    this.antallPlasser,
)
