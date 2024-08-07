package no.nav.amt.deltaker.bff.application.plugins

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson { applicationConfig() }
    }
}

fun ObjectMapper.applicationConfig() {
    registerModule(JavaTimeModule())
    registerKotlinModule()
    enable(SerializationFeature.INDENT_OUTPUT)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

val objectMapper = jacksonObjectMapper().apply { applicationConfig() }

/**
 * Inkluderer type informasjon som er definert av @JsonTypeInfo i lister og andre samlinger
 *
 * Hvis man bruker `writeValueAsString` på en `List<GeneriskType>` så vil den ikke inkludere `type`.
 */
inline fun <reified T> ObjectMapper.writePolymorphicListAsString(value: T): String =
    this.writerFor(object : TypeReference<T>() {}).writeValueAsString(value)
