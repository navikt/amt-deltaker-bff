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
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerDto
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavEnhetDto
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.utils.data.TestData

const val AMT_DELTAKER_URL = "http://amt-deltaker"
const val AMT_PERSON_URL = "http://amt-person-service"

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

fun mockAmtPersonClient() = AmtPersonServiceClient(
    baseUrl = AMT_PERSON_URL,
    scope = "amt.person-service.scope",
    httpClient = mockHttpClient(),
    azureAdTokenClient = mockAzureAdClient(),
)

fun mockAmtDeltakerClient() = AmtDeltakerClient(
    baseUrl = AMT_DELTAKER_URL,
    scope = "amt.deltaker.scope",
    httpClient = mockHttpClient(),
    azureAdTokenClient = mockAzureAdClient(),
)

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
        responseBody: Any,
        responseCode: HttpStatusCode = HttpStatusCode.OK,
    ) {
        val api = Pair(url, method)
        responses[api] = Response(
            if (responseBody is String) responseBody else objectMapper.writeValueAsString(responseBody),
            responseCode,
        )
    }

    fun addNavAnsattResponse(navAnsatt: NavAnsatt) {
        val url = "$AMT_PERSON_URL/api/nav-ansatt"
        addResponse(url, HttpMethod.Post, navAnsatt)
    }

    fun addNavEnhetResponse(navEnhet: NavEnhet) {
        val url = "$AMT_PERSON_URL/api/nav-enhet"
        addResponse(url, HttpMethod.Post, NavEnhetDto(navEnhet.id, navEnhet.enhetsnummer, navEnhet.navn))
    }

    fun addOpprettKladdResponse(deltaker: Deltaker?) {
        val url = "$AMT_DELTAKER_URL/pamelding"
        if (deltaker == null) {
            addResponse(url, HttpMethod.Post, "Noe gikk galt", HttpStatusCode.InternalServerError)
        } else {
            addResponse(
                url = url,
                method = HttpMethod.Post,
                responseBody = AmtDeltakerDto(
                    id = deltaker.id,
                    navBruker = deltaker.navBruker,
                    deltakerliste = deltaker.deltakerliste,
                    startdato = deltaker.startdato,
                    sluttdato = deltaker.sluttdato,
                    dagerPerUke = deltaker.dagerPerUke,
                    deltakelsesprosent = deltaker.deltakelsesprosent,
                    bakgrunnsinformasjon = deltaker.bakgrunnsinformasjon,
                    innhold = deltaker.innhold,
                    status = deltaker.status,
                    sistEndretAv = TestData.lagNavAnsatt(navIdent = deltaker.sistEndretAv),
                    sistEndretAvEnhet = TestData.lagNavEnhet(enhetsnummer = deltaker.sistEndretAvEnhet!!),
                    sistEndret = deltaker.sistEndret,
                    opprettet = deltaker.opprettet,
                ),
            )
        }
    }
}
