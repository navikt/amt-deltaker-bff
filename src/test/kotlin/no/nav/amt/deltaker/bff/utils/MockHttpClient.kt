package no.nav.amt.deltaker.bff.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData

fun mockHttpClient(response: String): HttpClient {
    val mockEngine = MockEngine {
        respond(
            content = ByteReadChannel(response),
            status = HttpStatusCode.OK,
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

fun mockAmtPersonServiceClient(navAnsatt: NavAnsatt = TestData.lagNavAnsatt()): AmtPersonServiceClient {
    return AmtPersonServiceClient(
        baseUrl = "https://amt-person-service",
        scope = "amt.person-service.scope",
        httpClient = mockHttpClient(objectMapper.writeValueAsString(navAnsatt)),
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
