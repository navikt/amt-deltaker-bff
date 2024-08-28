package no.nav.amt.deltaker.bff.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.utils.io.ByteReadChannel
import no.nav.amt.deltaker.bff.application.plugins.applicationConfig
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.arrangor.AmtArrangorClient
import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.arrangor.ArrangorDto
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Vedtak
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavEnhetDto
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.utils.data.TestData
import java.util.UUID

const val AMT_DELTAKER_URL = "http://amt-deltaker"
const val AMT_PERSON_SERVICE_URL = "http://amt-person-service"

fun mockHttpClient(defaultResponse: Any? = null): HttpClient {
    val mockEngine = MockEngine {
        val api = Pair(it.url.toString(), it.method)
        if (defaultResponse != null) MockResponseHandler.addResponse(it.url.toString(), it.method, defaultResponse)
        val response = MockResponseHandler.responses[api]!!

        respond(
            content = ByteReadChannel(response.content),
            status = response.status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    return HttpClient(mockEngine) {
        install(ContentNegotiation) {
            jackson { applicationConfig() }
        }
    }
}

fun mockAmtArrangorClient(arrangor: Arrangor = TestData.lagArrangor()): AmtArrangorClient {
    val overordnetArrangor = arrangor.overordnetArrangorId?.let {
        TestData.lagArrangor(id = arrangor.overordnetArrangorId!!)
    }

    val response = ArrangorDto(arrangor.id, arrangor.navn, arrangor.organisasjonsnummer, overordnetArrangor)
    return AmtArrangorClient(
        baseUrl = "https://amt-arrangor",
        scope = "amt.arrangor.scope",
        httpClient = mockHttpClient(objectMapper.writeValueAsString(response)),
        azureAdTokenClient = mockAzureAdClient(),
    )
}

fun mockAmtDeltakerClient() = AmtDeltakerClient(
    baseUrl = AMT_DELTAKER_URL,
    scope = "amt.deltaker.scope",
    httpClient = mockHttpClient(),
    azureAdTokenClient = mockAzureAdClient(),
)

fun mockAmtPersonServiceClient(navEnhet: NavEnhet = TestData.lagNavEnhet()): AmtPersonServiceClient {
    val navEnhetDto = NavEnhetDto(
        id = navEnhet.id,
        navn = navEnhet.navn,
        enhetId = navEnhet.enhetsnummer,
    )
    return AmtPersonServiceClient(
        baseUrl = AMT_PERSON_SERVICE_URL,
        scope = "amt.personservice.scope",
        httpClient = mockHttpClient(objectMapper.writeValueAsString(navEnhetDto)),
        azureAdTokenClient = mockAzureAdClient(),
    )
}

fun mockAzureAdClient() = AzureAdTokenClient(
    azureAdTokenUrl = "http://azure",
    clientId = "clientId",
    clientSecret = "secret",
    httpClient = mockHttpClient(
        """
        {
            "token_type":"Bearer",
            "access_token":"XYZ",
            "expires_in": 3599
        }
        """.trimIndent(),
    ),
)

object MockResponseHandler {
    data class Response(
        val content: String,
        val status: HttpStatusCode,
    )

    val responses = mutableMapOf<Pair<String, HttpMethod>, Response>()

    fun addResponse(
        url: String,
        method: HttpMethod,
        responseBody: Any = "",
        responseCode: HttpStatusCode = HttpStatusCode.OK,
    ) {
        val api = Pair(url, method)

        responses[api] = Response(
            if (responseBody is String) responseBody else objectMapper.writeValueAsString(responseBody),
            responseCode,
        )
    }

    fun addOpprettKladdResponse(deltaker: Deltaker?) {
        val url = "$AMT_DELTAKER_URL/pamelding"
        if (deltaker == null) {
            addResponse(url, HttpMethod.Post, "Noe gikk galt", HttpStatusCode.InternalServerError)
        } else {
            addResponse(
                url = url,
                method = HttpMethod.Post,
                responseBody = KladdResponse(
                    id = deltaker.id,
                    navBruker = deltaker.navBruker,
                    deltakerlisteId = deltaker.deltakerliste.id,
                    startdato = deltaker.startdato,
                    sluttdato = deltaker.sluttdato,
                    dagerPerUke = deltaker.dagerPerUke,
                    deltakelsesprosent = deltaker.deltakelsesprosent,
                    bakgrunnsinformasjon = deltaker.bakgrunnsinformasjon,
                    deltakelsesinnhold = deltaker.deltakelsesinnhold!!,
                    status = deltaker.status,
                ),
            )
        }
    }

    fun addUtkastResponse(deltaker: Deltaker) {
        val url = "$AMT_DELTAKER_URL/pamelding/${deltaker.id}"
        addResponse(
            url = url,
            method = HttpMethod.Post,
            responseBody = deltaker.toDeltakeroppdatering(),
        )
    }

    fun addSlettKladdResponse(deltakerId: UUID) {
        val url = "$AMT_DELTAKER_URL/pamelding/$deltakerId"
        addResponse(
            url = url,
            method = HttpMethod.Delete,
        )
    }

    fun addEndringsresponse(
        deltaker: Deltaker,
        endring: DeltakerEndring.Endring,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        val url = "$AMT_DELTAKER_URL/deltaker/${deltaker.id}/" + when (endring) {
            is DeltakerEndring.Endring.AvsluttDeltakelse -> AmtDeltakerClient.AVSLUTT_DELTAKELSE
            is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> AmtDeltakerClient.BAKGRUNNSINFORMASJON
            is DeltakerEndring.Endring.EndreDeltakelsesmengde -> AmtDeltakerClient.DELTAKELSESMENGDE
            is DeltakerEndring.Endring.EndreInnhold -> AmtDeltakerClient.INNHOLD
            is DeltakerEndring.Endring.EndreSluttarsak -> AmtDeltakerClient.SLUTTARSAK
            is DeltakerEndring.Endring.EndreSluttdato -> AmtDeltakerClient.SLUTTDATO
            is DeltakerEndring.Endring.EndreStartdato -> AmtDeltakerClient.STARTDATO
            is DeltakerEndring.Endring.ForlengDeltakelse -> AmtDeltakerClient.FORLENG_DELTAKELSE
            is DeltakerEndring.Endring.IkkeAktuell -> AmtDeltakerClient.IKKE_AKTUELL
            is DeltakerEndring.Endring.ReaktiverDeltakelse -> AmtDeltakerClient.REAKTIVER
        }

        addResponse(
            url = url,
            method = HttpMethod.Post,
            responseBody = deltaker.toDeltakeroppdatering(),
            responseCode = status,
        )
    }

    fun addFattVedtakResponse(
        deltaker: Deltaker,
        vedtak: Vedtak,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        val url = "$AMT_DELTAKER_URL/deltaker/${deltaker.id}/vedtak/${vedtak.id}/fatt"
        addResponse(url, HttpMethod.Post, deltaker.toDeltakeroppdatering(), status)
    }
}

fun Deltaker.toDeltakeroppdatering() = Deltakeroppdatering(
    id,
    startdato,
    sluttdato,
    dagerPerUke,
    deltakelsesprosent,
    bakgrunnsinformasjon,
    deltakelsesinnhold,
    status,
    historikk,
)
