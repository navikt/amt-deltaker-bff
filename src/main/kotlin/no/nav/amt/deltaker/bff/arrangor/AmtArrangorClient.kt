package no.nav.amt.deltaker.bff.arrangor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import org.slf4j.LoggerFactory
import java.util.UUID

class AmtArrangorClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun hentArrangor(orgnummer: String): ArrangorDto {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.get("$baseUrl/api/service/arrangor/organisasjonsnummer/$orgnummer") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke hente arrangør med orgnummer $orgnummer fra amt-arrangør. Status=${response.status.value}",
            )
        }
        return response.body()
    }

    suspend fun hentArrangor(id: UUID): ArrangorDto {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        log.info("dev token: $token")

        val response = httpClient.get("$baseUrl/api/service/arrangor/$id") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
        }
        log.info("Request: ${response.request}")
        log.info("Response: ${response.bodyAsText()}")

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke hente arrangør med id $id fra amt-arrangør. Status=${response.status.value} " +
                    "error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }
}

data class ArrangorDto(
    val id: UUID,
    val navn: String,
    val organisasjonsnummer: String,
    val overordnetArrangor: Arrangor?,
)
