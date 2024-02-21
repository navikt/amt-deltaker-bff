package no.nav.amt.deltaker.bff.deltaker.amtdeltaker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.OpprettKladdRequest
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import java.util.UUID

class AmtDeltakerClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {

    suspend fun opprettKladd(
        deltakerlisteId: UUID,
        personident: String,
        opprettetAv: String,
        opprettetAvEnhet: String,
    ): Deltaker {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(OpprettKladdRequest(deltakerlisteId, personident, opprettetAv, opprettetAvEnhet))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke opprette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body<AmtDeltakerDto>().toModel()
    }
}
