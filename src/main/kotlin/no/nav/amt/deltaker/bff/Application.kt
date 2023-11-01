package no.nav.amt.deltaker.bff

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.amt.deltaker.bff.application.isReadyKey
import no.nav.amt.deltaker.bff.application.plugins.applicationConfig
import no.nav.amt.deltaker.bff.application.plugins.configureMonitoring
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.application.registerInternalApi
import no.nav.amt.deltaker.bff.arrangor.AmtArrangorClient
import no.nav.amt.deltaker.bff.arrangor.ArrangorConsumer
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.db.Database

fun main() {
    val server = embeddedServer(Netty, port = 8080, module = Application::module)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 30_000)
        },
    )
    server.start(wait = true)
}

fun Application.module() {
    configureSerialization()

    val environment = Environment()

    Database.init(environment)

    val httpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            jackson { applicationConfig() }
        }
    }

    val azureAdTokenClient = AzureAdTokenClient(
        environment.azureAdTokenUrl,
        environment.azureClientId,
        environment.azureClientSecret,
        httpClient,
    )

    val amtArrangorClient = AmtArrangorClient(
        environment.amtArrangorUrl,
        environment.amtArrangorScope,
        httpClient,
        azureAdTokenClient,
    )

    val arrangorRepository = ArrangorRepository()

    val arrangorConsumer = ArrangorConsumer(arrangorRepository)
    arrangorConsumer.run()

    configureRouting()
    configureMonitoring()
    registerInternalApi(amtArrangorClient)

    attributes.put(isReadyKey, true)
}
