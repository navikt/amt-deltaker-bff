package no.nav.amt.deltaker.bff.utils

import io.getunleash.Unleash
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.apiclients.distribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.application.plugins.configureAuthentication
import no.nav.amt.deltaker.bff.application.plugins.configureRouting
import no.nav.amt.deltaker.bff.application.plugins.configureSerialization
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.innbygger.InnbyggerService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.sporbarhet.SporbarhetsloggService
import no.nav.amt.deltaker.bff.testdata.TestdataService
import no.nav.amt.deltaker.bff.tiltakskoordinator.SporbarhetOgTilgangskontrollSvc
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.UlestHendelseService
import no.nav.amt.lib.utils.applicationConfig
import org.junit.jupiter.api.BeforeEach

abstract class RouteTestBase {
    val tilgangskontrollService: TilgangskontrollService = mockk(relaxed = true)
    val deltakerService: DeltakerService = mockk(relaxed = true)
    val pameldingService: PameldingService = mockk(relaxed = true)
    val navAnsattService: NavAnsattService = mockk(relaxed = true)
    val navEnhetService: NavEnhetService = mockk(relaxed = true)
    val innbyggerService: InnbyggerService = mockk(relaxed = true)
    val forslagService: ForslagService = mockk(relaxed = true)
    val amtDistribusjonClient: AmtDistribusjonClient = mockk(relaxed = true)
    val sporbarhetsloggService: SporbarhetsloggService = mockk(relaxed = true)
    val deltakerRepository: DeltakerRepository = mockk(relaxed = true)
    val amtDeltakerClient: AmtDeltakerClient = mockk(relaxed = true)
    val deltakerlisteService: DeltakerlisteService = mockk(relaxed = true)
    val unleash: Unleash = mockk(relaxed = true)
    val sporbarhetOgTilgangskontrollSvc: SporbarhetOgTilgangskontrollSvc = mockk(relaxed = true)
    val tiltakskoordinatorService: TiltakskoordinatorService = mockk(relaxed = true)
    val ulestHendelseService: UlestHendelseService = mockk(relaxed = true)
    val testdataService: TestdataService = mockk(relaxed = true)

    @BeforeEach
    fun init() {
        clearAllMocks()
        configureEnvForAuthentication()
    }

    fun <T : Any> withTestApplicationContext(block: suspend (HttpClient) -> T): T {
        lateinit var result: T

        testApplication {
            application {
                configureSerialization()
                configureAuthentication(Environment())
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
                    sporbarhetOgTilgangskontrollSvc,
                    tiltakskoordinatorService,
                    ulestHendelseService,
                    testdataService,
                )
            }

            result =
                block(
                    createClient {
                        install(ContentNegotiation) {
                            jackson { applicationConfig() }
                        }
                    },
                )
        }

        return result
    }
}
