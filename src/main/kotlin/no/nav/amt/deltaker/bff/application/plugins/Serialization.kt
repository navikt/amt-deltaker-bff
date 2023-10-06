package no.nav.amt.deltaker.bff.application.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.jackson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
}
