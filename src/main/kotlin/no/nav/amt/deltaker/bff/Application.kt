package no.nav.amt.deltaker.bff

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
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
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorStengTilgangJob
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorsDeltakerlisteProducer
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingConsumer
import no.nav.amt.deltaker.bff.deltaker.forslag.kafka.ArrangorMeldingProducer
import no.nav.amt.deltaker.bff.deltaker.job.SlettUtdatertKladdJob
import no.nav.amt.deltaker.bff.deltaker.job.leaderelection.LeaderElection
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerV2Consumer
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerConsumer
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingRepository
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlisteConsumer
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka.TiltakstypeConsumer
import no.nav.amt.deltaker.bff.innbygger.InnbyggerService
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavAnsattConsumer
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.utils.database.Database
import no.nav.common.audit_log.log.AuditLoggerImpl
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

    Database.init(environment.databaseConfig)

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

    val amtDistribusjonClient = AmtDistribusjonClient(
        baseUrl = environment.amtDistribusjonUrl,
        scope = environment.amtDistribusjonScope,
        httpClient = httpClient,
        azureAdTokenClient = azureAdTokenClient,
    )

    val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(environment.appName)
            .instanceId(environment.appName)
            .unleashAPI("${environment.unleashUrl}/api")
            .apiKey(environment.unleashApiToken)
            .build(),
    )

    val kafkaProducer = Producer<String, String>(if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl())

    val arrangorRepository = ArrangorRepository()
    val deltakerlisteRepository = DeltakerlisteRepository()
    val navAnsattRepository = NavAnsattRepository()
    val navEnhetRepository = NavEnhetRepository()
    val navAnsattService = NavAnsattService(navAnsattRepository, amtPersonServiceClient)
    val navEnhetService = NavEnhetService(navEnhetRepository, amtPersonServiceClient)

    val navBrukerRepository = NavBrukerRepository()
    val navBrukerService = NavBrukerService(
        amtPersonServiceClient,
        navBrukerRepository,
        navAnsattService,
        navEnhetService,
    )

    val arrangorService = ArrangorService(arrangorRepository, amtArrangorClient)
    val deltakerlisteService = DeltakerlisteService(deltakerlisteRepository)

    val poaoTilgangCachedClient = PoaoTilgangCachedClient.createDefaultCacheClient(
        PoaoTilgangHttpClient(
            baseUrl = environment.poaoTilgangUrl,
            tokenProvider = { runBlocking { azureAdTokenClient.getMachineToMachineTokenWithoutType(environment.poaoTilgangScope) } },
        ),
    )

    val tiltakskoordinatorTilgangRepository = TiltakskoordinatorTilgangRepository()
    val tiltakskoordinatorsDeltakerlisteProducer = TiltakskoordinatorsDeltakerlisteProducer(kafkaProducer)

    val tilgangskontrollService = TilgangskontrollService(
        poaoTilgangCachedClient,
        navAnsattService,
        tiltakskoordinatorTilgangRepository,
        tiltakskoordinatorsDeltakerlisteProducer,
    )

    val sporbarhetsloggService = SporbarhetsloggService(AuditLoggerImpl())

    val deltakerRepository = DeltakerRepository()

    val forslagRepository = ForslagRepository()
    val forslagService = ForslagService(forslagRepository, navAnsattService, navEnhetService, ArrangorMeldingProducer(kafkaProducer))

    val vurderingRepository = VurderingRepository()
    val vurderingService = VurderingService(vurderingRepository)
    val deltakerService = DeltakerService(deltakerRepository, amtDeltakerClient, navEnhetService, forslagService)

    val pameldingService = PameldingService(
        deltakerService = deltakerService,
        navBrukerService = navBrukerService,
        amtDeltakerClient = amtDeltakerClient,
        navEnhetService = navEnhetService,
    )

    val innbyggerService = InnbyggerService(amtDeltakerClient, deltakerService)

    val tiltakskoordinatorService = TiltakskoordinatorService(
        amtDeltakerClient,
        deltakerService,
        tiltakskoordinatorTilgangRepository,
        vurderingService,
        navEnhetService,
        navAnsattService,
    )

    val tiltakstypeRepository = TiltakstypeRepository()

    val unleashToggle = UnleashToggle(unleash)
    val consumers = listOf(
        ArrangorConsumer(arrangorRepository),
        DeltakerlisteConsumer(deltakerlisteRepository, arrangorService, tiltakstypeRepository, pameldingService, tilgangskontrollService),
        NavAnsattConsumer(navAnsattService),
        NavBrukerConsumer(navBrukerService, pameldingService),
        TiltakstypeConsumer(tiltakstypeRepository),
        DeltakerV2Consumer(deltakerService, deltakerlisteRepository, vurderingService, navBrukerService, unleashToggle),
        ArrangorMeldingConsumer(forslagService),
    )
    consumers.forEach { it.run() }

    configureAuthentication(environment)
    configureRouting(
        tilgangskontrollService,
        deltakerService,
        pameldingService,
        navAnsattService,
        navEnhetService,
        innbyggerService,
        forslagService,
        amtDistribusjonClient,
        sporbarhetsloggService,
        deltakerRepository,
        amtDeltakerClient,
        deltakerlisteService,
        unleash,
        tiltakskoordinatorService,
    )
    configureMonitoring()

    val slettUtdatertKladdJob = SlettUtdatertKladdJob(leaderElection, attributes, deltakerRepository, pameldingService)
    slettUtdatertKladdJob.startJob()

    val tiltakskoordinatorStengTilgangJob = TiltakskoordinatorStengTilgangJob(leaderElection, attributes, tilgangskontrollService)
    tiltakskoordinatorStengTilgangJob.startJob()

    attributes.put(isReadyKey, true)
}
