package no.nav.amt.deltaker.bff.deltaker.amtdistribusjon

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import no.nav.amt.lib.ktor.auth.AzureAdTokenClient
import java.time.Duration

class AmtDistribusjonClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    private val digitalBrukerCache: Cache<String, Boolean> = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build(),
) {
    suspend fun digitalBruker(personident: String): Boolean {
        digitalBrukerCache.getIfPresent(personident)?.let {
            return it
        }
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/digital") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(DigitalBrukerRequest(personident))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke hente om bruker er digital fra amt-distribusjon. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        val digitalBruker = response.body<DigitalBrukerResponse>().erDigital
        digitalBrukerCache.put(personident, digitalBruker)
        return digitalBruker
    }
}

data class DigitalBrukerRequest(
    val personident: String,
)

data class DigitalBrukerResponse(
    val erDigital: Boolean,
)
