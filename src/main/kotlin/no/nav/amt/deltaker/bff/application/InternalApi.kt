package no.nav.amt.deltaker.bff.application

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.amt.deltaker.bff.arrangor.AmtArrangorClient
import java.util.UUID

fun Application.registerInternalApi(amtArrangorClient: AmtArrangorClient) {
    routing {
        get("internal/arrangor/{id}") {
            if (isInternalRequest(call.request)) {
                val id = UUID.fromString(call.parameters["id"])
                val arrangor = amtArrangorClient.hentArrangor(id)
                call.respond(arrangor.id)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

fun isInternalRequest(request: ApplicationRequest): Boolean {
    return request.origin.remoteHost == "127.0.0.1"
}
