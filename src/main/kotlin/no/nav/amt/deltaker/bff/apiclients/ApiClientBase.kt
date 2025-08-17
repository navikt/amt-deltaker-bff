package no.nav.amt.deltaker.bff.apiclients

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.exceptions.AuthenticationException
import no.nav.amt.deltaker.bff.auth.exceptions.AuthorizationException
import no.nav.amt.lib.ktor.auth.AzureAdTokenClient

abstract class ApiClientBase(
    protected val baseUrl: String,
    protected val scope: String,
    protected val httpClient: HttpClient,
    protected val azureAdTokenClient: AzureAdTokenClient,
) {
    protected suspend fun performPost(urlSubPath: String, requestBody: Any?): HttpResponse = httpClient.post("$baseUrl/$urlSubPath") {
        header(HttpHeaders.Authorization, azureAdTokenClient.getMachineToMachineToken(scope))
        accept(ContentType.Application.Json)
        if (requestBody != null) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
    }

    protected suspend fun HttpResponse.failIfNotSuccess(errorDescription: String): HttpResponse = if (!this.status.isSuccess()) {
        val bodyAsText = runCatching { bodyAsText() }.getOrElse { "Kunne ikke lese respons" }
        val fullErrorDescription = "$errorDescription Status=${this.status.value} error=$bodyAsText"

        when (this.status) {
            HttpStatusCode.Unauthorized -> throw AuthenticationException(fullErrorDescription)
            HttpStatusCode.Forbidden -> throw AuthorizationException(fullErrorDescription)
            HttpStatusCode.BadRequest -> throw IllegalArgumentException(fullErrorDescription)
            HttpStatusCode.NotFound -> throw NoSuchElementException(fullErrorDescription)
            else -> throw IllegalStateException(fullErrorDescription) // gir HttpStatusCode.InternalServerError i StatusPages
        }
    } else {
        this
    }
}
