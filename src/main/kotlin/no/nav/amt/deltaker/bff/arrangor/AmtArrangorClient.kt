package no.nav.amt.deltaker.bff.arrangor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import java.util.UUID

class AmtArrangorClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    suspend fun hentArrangor(orgnummer: String): ArrangorDto {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.get("$baseUrl/api/service/arrangor/organisasjonsnummer/$orgnummer") {
            headers {
                HttpHeaders.Authorization to token
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
        val response = httpClient.get("$baseUrl/api/service/arrangor/$id") {
            headers {
                HttpHeaders.Authorization to token
            }
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke hente arrangør med id $id fra amt-arrangør. Status=${response.status.value}",
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
