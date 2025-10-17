package no.nav.amt.deltaker.bff.deltakerliste.kafka

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlistePayload.Companion.ENKELTPLASS_V2_TYPE
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlistePayload.Companion.GRUPPE_V2_TYPE
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.lagArrangor
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerlistePayload
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtArrangorClient
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.mockPaameldingClient
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeltakerlisteConsumerTest {
    companion object {
        private val deltakerlisteRepository = DeltakerlisteRepository()
        private val tiltakstypeRepository = TiltakstypeRepository()
        private val tilgangskontrollService: TilgangskontrollService = mockk(relaxed = true)
        private val unleashToggle: UnleashToggle = mockk(relaxed = true)

        lateinit var navEnhetService: NavEnhetService
        lateinit var deltakerService: DeltakerService
        lateinit var pameldingService: PameldingService
        lateinit var navAnsattService: NavAnsattService

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container

            navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
            navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
            deltakerService = DeltakerService(
                deltakerRepository = DeltakerRepository(),
                amtDeltakerClient = mockAmtDeltakerClient(),
                paameldingClient = mockPaameldingClient(),
                navEnhetService = navEnhetService,
                forslagService = mockk(relaxed = true),
            )
            pameldingService = PameldingService(
                deltakerService = deltakerService,
                navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
                navEnhetService = navEnhetService,
                paameldingClient = mockPaameldingClient(),
            )
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
        clearAllMocks()
        every { unleashToggle.skalLeseGjennomforingerV2() } returns true
        every { unleashToggle.skalLeseArenaDataForTiltakstype(any<String>()) } returns true
    }

    @Test
    fun `unleashToggle er ikke enabled for gjennomforingV2 - lagrer ikke deltakerliste`() {
        every { unleashToggle.skalLeseGjennomforingerV2() } returns false

        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)
        TestRepository.insert(tiltakstype)

        val arrangor = lagArrangor()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
                topic = Environment.DELTAKERLISTE_V2_TOPIC,
            )

        val expectedDeltakerliste = lagDeltakerliste(arrangor = arrangor, tiltakstype = tiltakstype)
        val deltakerlistePayload = lagDeltakerlistePayload(arrangor, expectedDeltakerliste).copy(
            type = GRUPPE_V2_TYPE,
            virksomhetsnummer = null,
            arrangor = DeltakerlistePayload.Arrangor(arrangor.organisasjonsnummer),
        )

        runBlocking {
            consumer.consume(
                deltakerlistePayload.id,
                objectMapper.writeValueAsString(deltakerlistePayload),
            )

            val thrown = shouldThrow<NoSuchElementException> {
                deltakerlisteRepository.get(expectedDeltakerliste.id).getOrThrow()
            }

            thrown.message shouldBe "Fant ikke deltakerliste med id ${expectedDeltakerliste.id}"
        }
    }

    @Test
    fun `ny liste v2 gruppe - lagrer deltakerliste`() {
        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)
        TestRepository.insert(tiltakstype)

        val arrangor = lagArrangor()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
                topic = Environment.DELTAKERLISTE_V2_TOPIC,
            )

        val expectedDeltakerliste = lagDeltakerliste(arrangor = arrangor, tiltakstype = tiltakstype)
        val deltakerlistePayload = lagDeltakerlistePayload(arrangor, expectedDeltakerliste).copy(
            type = GRUPPE_V2_TYPE,
            virksomhetsnummer = null,
            arrangor = DeltakerlistePayload.Arrangor(arrangor.organisasjonsnummer),
        )

        runBlocking {
            consumer.consume(
                deltakerlistePayload.id,
                objectMapper.writeValueAsString(deltakerlistePayload),
            )

            deltakerlisteRepository.get(expectedDeltakerliste.id).getOrThrow() shouldBe expectedDeltakerliste
        }

        coVerify(exactly = 0) { tilgangskontrollService.stengTilgangerTilDeltakerliste(any()) }
    }

    @Test
    fun `ny liste v2 enkeltplass - lagrer deltakerliste`() {
        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING)
        TestRepository.insert(tiltakstype)

        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor, tiltakstype = tiltakstype)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
                topic = Environment.DELTAKERLISTE_V2_TOPIC,
            )

        val deltakerlistePayload = lagDeltakerlistePayload(arrangor, deltakerliste).copy(
            type = ENKELTPLASS_V2_TYPE,
            navn = null,
            startDato = null,
            sluttDato = null,
            status = null,
            oppstart = null,
            virksomhetsnummer = null,
            arrangor = DeltakerlistePayload.Arrangor(arrangor.organisasjonsnummer),
        )

        runBlocking {
            consumer.consume(
                deltakerlistePayload.id,
                objectMapper.writeValueAsString(deltakerlistePayload),
            )

            deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe deltakerliste.copy(
                navn = "Test tiltak ENKFAGYRKE",
                status = null,
                startDato = null,
                sluttDato = null,
                oppstart = null,
            )
        }

        coVerify(exactly = 0) { tilgangskontrollService.stengTilgangerTilDeltakerliste(any()) }
    }

    @Test
    fun `consumeDeltakerliste - ny liste og arrangor - lagrer deltakerliste`() {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))
        val consumer = DeltakerlisteConsumer(
            deltakerlisteRepository,
            arrangorService,
            tiltakstypeRepository,
            pameldingService,
            tilgangskontrollService,
            unleashToggle = unleashToggle,
            Environment.DELTAKERLISTE_V1_TOPIC,
        )

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(lagDeltakerlistePayload(arrangor, deltakerliste)),
            )

            deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe deltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - ny sluttdato - oppdaterer deltakerliste`() {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(
            deltakerlisteRepository,
            arrangorService,
            tiltakstypeRepository,
            pameldingService,
            tilgangskontrollService,
            unleashToggle = unleashToggle,
            Environment.DELTAKERLISTE_V1_TOPIC,
        )

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now())

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(lagDeltakerlistePayload(arrangor, oppdatertDeltakerliste)),
            )

            deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe oppdatertDeltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - tombstone - sletter deltakerliste`() {
        val deltakerliste = lagDeltakerliste()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())

        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(
            deltakerlisteRepository,
            arrangorService,
            tiltakstypeRepository,
            pameldingService,
            tilgangskontrollService,
            unleashToggle = unleashToggle,
            Environment.DELTAKERLISTE_V1_TOPIC,
        )

        runBlocking {
            consumer.consume(deltakerliste.id, null)

            deltakerlisteRepository.get(deltakerliste.id).getOrNull() shouldBe null
        }
    }

    @Test
    fun `consumeDeltakerliste - avbrutt, finnes deltakere - oppdaterer deltakerliste, sletter kladd`() {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)
        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltakerliste)
        TestRepository.insert(kladd)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltaker)
        MockResponseHandler.addSlettKladdResponse(kladd.id)

        val consumer = DeltakerlisteConsumer(
            deltakerlisteRepository,
            arrangorService,
            tiltakstypeRepository,
            pameldingService,
            tilgangskontrollService,
            unleashToggle = unleashToggle,
            Environment.DELTAKERLISTE_V1_TOPIC,
        )

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now(), status = Deltakerliste.Status.AVBRUTT)

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(lagDeltakerlistePayload(arrangor, oppdatertDeltakerliste)),
            )

            deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe oppdatertDeltakerliste
            deltakerService.getDeltaker(kladd.id).getOrNull() shouldBe null
            deltakerService.getDeltaker(deltaker.id).getOrNull() shouldNotBe null
        }
    }
}
