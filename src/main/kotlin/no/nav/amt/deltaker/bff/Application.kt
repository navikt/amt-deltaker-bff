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
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerConsumer
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteConsumer
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka.TiltakstypeConsumer
import no.nav.amt.deltaker.bff.endringsmelding.EndringsmeldingConsumer
import no.nav.amt.deltaker.bff.endringsmelding.EndringsmeldingRepository
import no.nav.amt.deltaker.bff.endringsmelding.EndringsmeldingService
import no.nav.amt.deltaker.bff.job.DeltakerStatusOppdateringService
import no.nav.amt.deltaker.bff.job.StatusUpdateJob
import no.nav.amt.deltaker.bff.job.leaderelection.LeaderElection
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavAnsattConsumer
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
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

    val leaderElection = LeaderElection(httpClient, environment.electorPath)

    val azureAdTokenClient = AzureAdTokenClient(
        azureAdTokenUrl = environment.azureAdTokenUrl,
        clientId = environment.azureClientId,
        clientSecret = environment.azureClientSecret,
        httpClient = httpClient,
    )

    val amtArrangorClient = AmtArrangorClient(
        baseUrl = environment.amtArrangorUrl,
        scope = environment.amtArrangorScope,
        httpClient = httpClient,
        azureAdTokenClient = azureAdTokenClient,
    )

    val amtPersonServiceClient = AmtPersonServiceClient(
        baseUrl = environment.amtPersonServiceUrl,
        scope = environment.amtPersonServiceScope,
        httpClient = httpClient,
        azureAdTokenClient = azureAdTokenClient,
    )

    val amtDeltakerClient = AmtDeltakerClient(
        baseUrl = environment.amtDeltakerUrl,
        scope = environment.amtDeltakerScope,
        httpClient = httpClient,
        azureAdTokenClient = azureAdTokenClient,
    )

    val arrangorRepository = ArrangorRepository()
    val deltakerlisteRepository = DeltakerlisteRepository()
    val navAnsattRepository = NavAnsattRepository()
    val navEnhetRepository = NavEnhetRepository()

    val navBrukerRepository = NavBrukerRepository()
    val navBrukerService = NavBrukerService(
        navBrukerRepository,
    )

    val arrangorService = ArrangorService(arrangorRepository, amtArrangorClient)
    val navAnsattService = NavAnsattService(navAnsattRepository, amtPersonServiceClient)
    val navEnhetService = NavEnhetService(navEnhetRepository, amtPersonServiceClient)

    val poaoTilgangCachedClient = PoaoTilgangCachedClient.createDefaultCacheClient(
        PoaoTilgangHttpClient(
            baseUrl = environment.poaoTilgangUrl,
            tokenProvider = { runBlocking { azureAdTokenClient.getMachineToMachineTokenWithoutType(environment.poaoTilgangScope) } },
        ),
    )
    val tilgangskontrollService = TilgangskontrollService(poaoTilgangCachedClient)
    val deltakerRepository = DeltakerRepository()

    val deltakerService = DeltakerService(
        deltakerRepository,
        navAnsattService,
        navEnhetService,
    )

    val pameldingService = PameldingService(
        deltakerService = deltakerService,
        navBrukerService = navBrukerService,
        amtDeltakerClient = amtDeltakerClient,
    )

    val endringsmeldingRepository = EndringsmeldingRepository()
    val endringsmeldingService = EndringsmeldingService(deltakerService, navAnsattService, endringsmeldingRepository)

    val tiltakstypeRepository = TiltakstypeRepository()

    val deltakerStatusOppdateringService = DeltakerStatusOppdateringService(deltakerRepository)

    val consumers = listOf(
        EndringsmeldingConsumer(endringsmeldingService),
        ArrangorConsumer(arrangorRepository),
        DeltakerlisteConsumer(deltakerlisteRepository, arrangorService, tiltakstypeRepository),
        NavAnsattConsumer(navAnsattService),
        NavBrukerConsumer(navBrukerRepository),
        TiltakstypeConsumer(tiltakstypeRepository),
    )
    consumers.forEach { it.run() }

    configureAuthentication(environment)
    configureRouting(
        tilgangskontrollService,
        deltakerService,
        pameldingService,
        navAnsattService,
        navEnhetService,
    )
    configureMonitoring()

    val statusUpdateJob = StatusUpdateJob(leaderElection, attributes, deltakerStatusOppdateringService)
    statusUpdateJob.startJob()

    attributes.put(isReadyKey, true)
}
