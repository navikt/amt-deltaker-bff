package no.nav.amt.deltaker.bff

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.Environment.Companion.HTTP_CLIENT_TIMEOUT_MS
import no.nav.amt.deltaker.bff.application.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.application.deltakerliste.kafka.DeltakerlisteConsumer
import no.nav.amt.deltaker.bff.application.isReadyKey
import no.nav.amt.deltaker.bff.application.plugins.applicationConfig
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureMonitoring
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.arrangor.AmtArrangorClient
import no.nav.amt.deltaker.bff.arrangor.ArrangorConsumer
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.db.Database
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient

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
        engine {
            socketTimeout = HTTP_CLIENT_TIMEOUT_MS
            connectTimeout = HTTP_CLIENT_TIMEOUT_MS
            connectionRequestTimeout = HTTP_CLIENT_TIMEOUT_MS * 2
        }
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
    val deltakerlisteRepository = DeltakerlisteRepository()

    val arrangorService = ArrangorService(arrangorRepository, amtArrangorClient)

    val arrangorConsumer = ArrangorConsumer(arrangorRepository)
    val deltakerlisteConsumer = DeltakerlisteConsumer(deltakerlisteRepository, arrangorService)

    arrangorConsumer.run()
    deltakerlisteConsumer.run()

    val poaoTilgangCachedClient = PoaoTilgangCachedClient.createDefaultCacheClient(
        PoaoTilgangHttpClient(
            baseUrl = environment.poaoTilgangUrl,
            tokenProvider = { runBlocking { azureAdTokenClient.getMachineToMachineToken(environment.poaoTilgangScope) } },
        ),
    )
    val tilgangskontrollService = TilgangskontrollService(poaoTilgangCachedClient)

    configureAuthentication(environment)
    configureRouting()
    configureMonitoring()

    attributes.put(isReadyKey, true)
}
