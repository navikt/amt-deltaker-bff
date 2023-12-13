package no.nav.amt.deltaker.bff.navansatt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AmtPersonServiceClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentNavAnsatt(navIdent: String): NavAnsatt {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/api/nav-ansatt") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(NavAnsattRequest(navIdent)))
        }
        if (!response.status.isSuccess()) {
            log.error(
                "Kunne ikke hente nav-ansatt med ident $navIdent fra amt-person-service. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
            throw RuntimeException("Kunne ikke hente NAV-ansatt fra amt-perosn-service")
        }
        return response.body()
    }
}

data class NavAnsattRequest(
    val navIdent: String,
)
