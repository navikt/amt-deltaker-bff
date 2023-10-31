package no.nav.amt.deltaker.bff.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.Parameters
import java.time.Duration

class AzureAdTokenClient(
    private val azureAdTokenUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val httpClient: HttpClient,
) {

    private val tokenCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(55))
        .build<String, AzureAdToken>()

    suspend fun getMachineToMachineToken(scope: String): String {
        val token = tokenCache.getIfPresent(scope) ?: createMachineToMachineToken(scope)

        return "${token.tokenType} ${token.accessToken}" // i.e. "Bearer XYZ"
    }

    private suspend fun createMachineToMachineToken(scope: String): AzureAdToken {
        val token: AzureAdToken = httpClient.post(azureAdTokenUrl) {
            FormDataContent(
                Parameters.build {
                    "grant_type" to "client_credentials"
                    "client_id" to clientId
                    "client_secret" to clientSecret
                    "scope" to scope
                },
            )
        }.body()

        tokenCache.put(scope, token)

        return token
    }
}

@JsonNaming(SnakeCaseStrategy::class)
data class AzureAdToken(
    val tokenType: String,
    val accessToken: String,
)
