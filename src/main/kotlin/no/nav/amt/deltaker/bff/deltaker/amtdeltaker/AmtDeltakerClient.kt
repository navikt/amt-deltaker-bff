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
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.BakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.DeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.InnholdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.OpprettKladdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.SluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.SluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.StartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import java.time.LocalDate
import java.util.UUID

class AmtDeltakerClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    suspend fun opprettKladd(deltakerlisteId: UUID, personident: String): KladdResponse {
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

    suspend fun avbrytUtkast(
        deltakerId: UUID,
        avbruttAv: String,
        avbruttAvEnhet: String,
    ) {
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
    ) = postEndring(
        deltakerId = deltakerId,
        request = BakgrunnsinformasjonRequest(endretAv, endretAvEnhet, bakgrunnsinformasjon),
        endepunkt = BAKGRUNNSINFORMASJON,
    )

    suspend fun endreInnhold(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        innhold: List<Innhold>,
    ) = postEndring(deltakerId, InnholdRequest(endretAv, endretAvEnhet, innhold), INNHOLD)

    suspend fun endreDeltakelsesmengde(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        deltakelsesprosent: Float?,
        dagerPerUke: Float?,
    ) = postEndring(
        deltakerId,
        DeltakelsesmengdeRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            deltakelsesprosent = deltakelsesprosent?.toInt(),
            dagerPerUke = dagerPerUke?.toInt(),
        ),
        DELTAKELSESMENGDE,
    )

    suspend fun endreStartdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        startdato: LocalDate?,
        sluttdato: LocalDate?,
    ) = postEndring(deltakerId, StartdatoRequest(endretAv, endretAvEnhet, startdato, sluttdato), STARTDATO)

    suspend fun endreSluttdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
    ) = postEndring(deltakerId, SluttdatoRequest(endretAv, endretAvEnhet, sluttdato), SLUTTDATO)

    suspend fun endreSluttaarsak(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
    ) = postEndring(deltakerId, SluttarsakRequest(endretAv, endretAvEnhet, aarsak), SLUTTARSAK)

    suspend fun forlengDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
    ) = postEndring(deltakerId, ForlengDeltakelseRequest(endretAv, endretAvEnhet, sluttdato), FORLENG_DELTAKELSE)

    suspend fun ikkeAktuell(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
    ) = postEndring(deltakerId, IkkeAktuellRequest(endretAv, endretAvEnhet, aarsak), IKKE_AKTUELL)

    suspend fun avsluttDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak,
    ) = postEndring(
        deltakerId,
        AvsluttDeltakelseRequest(endretAv, endretAvEnhet, sluttdato, aarsak),
        AVSLUTT_DELTAKELSE,
    )

    suspend fun fattVedtak(vedtak: Vedtak): Deltakeroppdatering {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/deltaker/${vedtak.deltakerId}/vedtak/${vedtak.id}/fatt") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke fatte vedtak i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }

        return response.body()
    }

    private suspend fun postEndring(
        deltakerId: UUID,
        request: Any,
        endepunkt: String,
    ): Deltakeroppdatering {
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

        return response.body()
    }

    companion object Endepunkt {
        const val BAKGRUNNSINFORMASJON = "bakgrunnsinformasjon"
        const val INNHOLD = "innhold"
        const val DELTAKELSESMENGDE = "deltakelsesmengde"
        const val STARTDATO = "startdato"
        const val SLUTTDATO = "sluttdato"
        const val SLUTTARSAK = "sluttarsak"
        const val FORLENG_DELTAKELSE = "forleng"
        const val IKKE_AKTUELL = "ikke-aktuell"
        const val AVSLUTT_DELTAKELSE = "avslutt"
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
