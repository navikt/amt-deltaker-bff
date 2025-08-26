package no.nav.amt.deltaker.bff.internal

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.lib.ktor.auth.exceptions.AuthorizationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Routing.registerInternalApi(deltakerRepository: DeltakerRepository, amtDeltakerClient: AmtDeltakerClient) {
    val scope = CoroutineScope(Dispatchers.IO)

    val log: Logger = LoggerFactory.getLogger(javaClass)

    post("/internal/synk-status") {
        if (isInternal(call.request.local.remoteAddress)) {
            scope.launch {
                log.info("Henter deltakelser med mer enn en gyldig status")
                val deltakerIder = deltakerRepository.getDeltakereMedFlereGyldigeStatuser()
                deltakerIder.forEach {
                    val deltaker = amtDeltakerClient.getDeltaker(it)
                    deltakerRepository.deaktiverUkritiskTidligereStatuserQuery(deltaker.status, it)
                    log.info("Oppdatert status for deltaker $it")
                }
                log.info("Deaktivert statuser for ${deltakerIder.size} deltakere")
            }
            call.respond(HttpStatusCode.OK)
        } else {
            throw AuthorizationException("Ikke tilgang til api")
        }
    }
}

fun isInternal(remoteAdress: String): Boolean = remoteAdress == "127.0.0.1"
