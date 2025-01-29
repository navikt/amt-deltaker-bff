package no.nav.amt.deltaker.bff.deltaker.api.utils

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.utils.generateJWT
import java.util.UUID

internal fun HttpRequestBuilder.postRequest(body: Any) {
    header(
        HttpHeaders.Authorization,
        "Bearer ${
            generateJWT(
                consumerClientId = "frontend-clientid",
                navAnsattAzureId = UUID.randomUUID().toString(),
                audience = "deltaker-bff",
            )
        }",
    )
    header("aktiv-enhet", "0101")
    contentType(ContentType.Application.Json)
    setBody(objectMapper.writeValueAsString(body))
}

internal fun HttpRequestBuilder.noBodyRequest() {
    header(
        HttpHeaders.Authorization,
        "Bearer ${
            generateJWT(
                consumerClientId = "frontend-clientid",
                navAnsattAzureId = UUID.randomUUID().toString(),
                audience = "deltaker-bff",
            )
        }",
    )
    header("aktiv-enhet", "0101")
}


internal fun HttpRequestBuilder.noBodyTiltakskoordinatorRequest() {
    header(
        HttpHeaders.Authorization,
        "Bearer ${
            generateJWT(
                consumerClientId = "frontend-clientid",
                navAnsattAzureId = UUID.randomUUID().toString(),
                audience = "deltaker-bff",
                groups = listOf(UUID(0L, 0L).toString()),
                
            )
        }",
    )
    header("aktiv-enhet", "0101")
}
