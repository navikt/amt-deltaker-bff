package no.nav.amt.deltaker.bff.deltaker.amtdeltaker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.AvbrytUtkastRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.BakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.DeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.InnholdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.OpprettKladdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
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
    ): KladdResponse {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(OpprettKladdRequest(deltakerlisteId, personident))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke opprette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }

    suspend fun utkast(utkast: Utkast) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding/${utkast.deltakerId}") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(utkast.toRequest())
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke oppdatere utkast i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    suspend fun slettKladd(deltakerId: UUID) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.delete("$baseUrl/pamelding/$deltakerId") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke slette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    suspend fun avbrytUtkast(deltakerId: UUID, avbruttAv: String, avbruttAvEnhet: String) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding/$deltakerId/avbryt") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(AvbrytUtkastRequest(avbruttAv, avbruttAvEnhet))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke avbryte utkast i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    suspend fun endreBakgrunnsinformasjon(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        bakgrunnsinformasjon: String?,
    ) {
        postEndring(
            deltakerId = deltakerId,
            request = BakgrunnsinformasjonRequest(endretAv, endretAvEnhet, bakgrunnsinformasjon),
            endepunkt = BAKGRUNNSINFORMASJON,
        )
    }

    suspend fun endreInnhold(deltakerId: UUID, endretAv: String, endretAvEnhet: String, innhold: List<Innhold>) {
        postEndring(deltakerId, InnholdRequest(endretAv, endretAvEnhet, innhold), INNHOLD)
    }

    suspend fun endreDeltakelsesmengde(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        deltakelsesprosent: Float?,
        dagerPerUke: Float?,
    ) {
        postEndring(
            deltakerId,
            DeltakelsesmengdeRequest(
                endretAv = endretAv,
                endretAvEnhet = endretAvEnhet,
                deltakelsesprosent = deltakelsesprosent?.toInt(),
                dagerPerUke = dagerPerUke?.toInt(),
            ),
            DELTAKELSESMENGDE,
        )
    }

    private suspend fun postEndring(
        deltakerId: UUID,
        request: Any,
        endepunkt: String,
    ) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/deltaker/$deltakerId/$endepunkt") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke endre $endepunkt i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    companion object Endepunkt {
        const val BAKGRUNNSINFORMASJON = "bakgrunnsinformasjon"
        const val INNHOLD = "innhold"
        const val DELTAKELSESMENGDE = "deltakelsesmengde"
    }
}

private fun Utkast.toRequest() = UtkastRequest(
    innhold = this.pamelding.innhold,
    bakgrunnsinformasjon = this.pamelding.bakgrunnsinformasjon,
    deltakelsesprosent = this.pamelding.deltakelsesprosent,
    dagerPerUke = this.pamelding.dagerPerUke,
    endretAv = this.pamelding.endretAv,
    endretAvEnhet = this.pamelding.endretAvEnhet,
    godkjentAvNav = this.godkjentAvNav,
)
