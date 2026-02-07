package no.nav.amt.deltaker.bff.deltakerliste.kafka

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.amt.deltaker.bff.DatabaseTestExtension
import no.nav.amt.deltaker.bff.arrangor.ArrangorRepository
import no.nav.amt.deltaker.bff.arrangor.ArrangorService
import no.nav.amt.deltaker.bff.auth.TilgangskontrollService
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.PameldingService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagEnkeltplassDeltakerlistePayload
import no.nav.amt.deltaker.bff.utils.data.TestData.lagGruppeDeltakerlistePayload
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtArrangorClient
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.mockPaameldingClient
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltakerliste.GjennomforingPameldingType
import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.kafka.GjennomforingV2KafkaPayload
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.testing.utils.TestData.lagArrangor
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

class DeltakerlisteConsumerTest {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseTestExtension()
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { unleashToggle.skipProsesseringAvGjennomforing(any<String>()) } returns false
    }

    @Test
    fun `unleashToggle er ikke enabled for tiltakstype - lagrer ikke deltakerliste`() = runTest {
        every { unleashToggle.skipProsesseringAvGjennomforing(any<String>()) } returns true

        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)
        tiltakstypeRepository.upsert(tiltakstype)

        val arrangor = lagArrangor()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerRepository = deltakerRepository,
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
            )

        val expectedDeltakerliste = lagDeltakerliste(arrangor = arrangor, tiltakstype = tiltakstype)
        val deltakerlistePayload = lagGruppeDeltakerlistePayload(arrangor, expectedDeltakerliste).copy(
            arrangor = GjennomforingV2KafkaPayload.Arrangor(arrangor.organisasjonsnummer),
        )

        consumer.consume(
            deltakerlistePayload.id,
            objectMapper.writeValueAsString(deltakerlistePayload),
        )

        val thrown = shouldThrow<NoSuchElementException> {
            deltakerlisteRepository.get(expectedDeltakerliste.id).getOrThrow()
        }

        thrown.message shouldBe "Fant ikke deltakerliste med id ${expectedDeltakerliste.id}"
    }

    @Test
    fun `ny liste gruppe - lagrer deltakerliste`() = runTest {
        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)
        tiltakstypeRepository.upsert(tiltakstype)

        val arrangor = lagArrangor()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerRepository = deltakerRepository,
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
            )

        val expectedDeltakerliste = lagDeltakerliste(
            arrangor = arrangor,
            tiltakstype = tiltakstype,
            oppstart = Oppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        )

        val deltakerlistePayload = lagGruppeDeltakerlistePayload(arrangor, expectedDeltakerliste).copy(
            arrangor = GjennomforingV2KafkaPayload.Arrangor(arrangor.organisasjonsnummer),
        )

        consumer.consume(
            deltakerlistePayload.id,
            objectMapper.writeValueAsString(deltakerlistePayload),
        )

        deltakerlisteRepository.get(expectedDeltakerliste.id).getOrThrow() shouldBe expectedDeltakerliste

        verify(exactly = 0) { tilgangskontrollService.stengTilgangerTilDeltakerliste(any()) }
    }

    @Test
    fun `ny liste v2 enkeltplass - lagrer deltakerliste`() = runTest {
        val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING)
        tiltakstypeRepository.upsert(tiltakstype)

        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(
            arrangor = arrangor,
            tiltakstype = tiltakstype,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        )
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))

        val consumer =
            DeltakerlisteConsumer(
                deltakerRepository = deltakerRepository,
                deltakerlisteRepository = deltakerlisteRepository,
                arrangorService = arrangorService,
                tiltakstypeRepository = tiltakstypeRepository,
                pameldingService = pameldingService,
                tilgangskontrollService = tilgangskontrollService,
                unleashToggle = unleashToggle,
            )

        val deltakerlistePayload = lagEnkeltplassDeltakerlistePayload(arrangor, deltakerliste)

        consumer.consume(
            deltakerlistePayload.id,
            objectMapper.writeValueAsString(deltakerlistePayload),
        )

        deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe deltakerliste.copy(
            navn = tiltakstype.navn,
            status = null,
            startDato = null,
            sluttDato = null,
            oppstart = null,
            antallPlasser = null,
            apentForPamelding = true,
            oppmoteSted = null,
        )

        verify(exactly = 0) { tilgangskontrollService.stengTilgangerTilDeltakerliste(any()) }
    }

    @Test
    fun `consumeDeltakerliste - ny liste og arrangor - lagrer deltakerliste`() = runTest {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor, pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK)
        tiltakstypeRepository.upsert(deltakerliste.tiltak)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))
        val consumer = DeltakerlisteConsumer(
            deltakerRepository = deltakerRepository,
            deltakerlisteRepository = deltakerlisteRepository,
            arrangorService = arrangorService,
            tiltakstypeRepository = tiltakstypeRepository,
            pameldingService = pameldingService,
            tilgangskontrollService = tilgangskontrollService,
            unleashToggle = unleashToggle,
        )

        consumer.consume(
            deltakerliste.id,
            objectMapper.writeValueAsString(lagGruppeDeltakerlistePayload(arrangor, deltakerliste)),
        )

        deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe deltakerliste
    }

    @Test
    fun `consumeDeltakerliste - ny sluttdato - oppdaterer deltakerliste`() = runTest {
        val arrangor = lagArrangor()
        val deltakerliste = lagDeltakerliste(arrangor = arrangor, pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(
            deltakerRepository = deltakerRepository,
            deltakerlisteRepository = deltakerlisteRepository,
            arrangorService = arrangorService,
            tiltakstypeRepository = tiltakstypeRepository,
            pameldingService = pameldingService,
            tilgangskontrollService = tilgangskontrollService,
            unleashToggle = unleashToggle,
        )

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now())

        consumer.consume(
            deltakerliste.id,
            objectMapper.writeValueAsString(lagGruppeDeltakerlistePayload(arrangor, oppdatertDeltakerliste)),
        )

        deltakerlisteRepository.get(deltakerliste.id).getOrThrow() shouldBe oppdatertDeltakerliste
    }

    @Test
    fun `consumeDeltakerliste - tombstone - sletter deltakerliste`() = runTest {
        val deltakerliste = lagDeltakerliste()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())

        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(
            deltakerRepository = deltakerRepository,
            deltakerlisteRepository = deltakerlisteRepository,
            arrangorService = arrangorService,
            tiltakstypeRepository = tiltakstypeRepository,
            pameldingService = pameldingService,
            tilgangskontrollService = tilgangskontrollService,
            unleashToggle = unleashToggle,
        )

        consumer.consume(deltakerliste.id, null)

        deltakerlisteRepository.get(deltakerliste.id).getOrNull() shouldBe null
    }

    @Test
    fun `consumeDeltakerliste - avbrutt, finnes deltakere - oppdaterer deltakerliste, sletter kladd`() = runTest {
        val arrangorInTest = lagArrangor()
        val deltakerlisteInTest = lagDeltakerliste(arrangor = arrangorInTest, pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK)

        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerlisteInTest)

        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltakerlisteInTest)
        TestRepository.insert(kladd)

        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltaker)

        MockResponseHandler.addSlettKladdResponse(kladd.id)

        val consumer = DeltakerlisteConsumer(
            deltakerRepository = deltakerRepository,
            deltakerlisteRepository = deltakerlisteRepository,
            arrangorService = arrangorService,
            tiltakstypeRepository = tiltakstypeRepository,
            pameldingService = pameldingService,
            tilgangskontrollService = tilgangskontrollService,
            unleashToggle = unleashToggle,
        )

        val mutatedDeltakerliste = deltakerlisteInTest.copy(sluttDato = LocalDate.now(), status = GjennomforingStatusType.AVBRUTT)

        consumer.consume(
            deltakerlisteInTest.id,
            objectMapper.writeValueAsString(lagGruppeDeltakerlistePayload(arrangorInTest, mutatedDeltakerliste)),
        )

        deltakerlisteRepository.get(deltakerlisteInTest.id).getOrThrow() shouldBe mutatedDeltakerliste
        deltakerRepository.get(kladd.id).getOrNull() shouldBe null
        deltakerRepository.get(deltaker.id).getOrNull() shouldNotBe null
    }

    private val deltakerlisteRepository = DeltakerlisteRepository()
    private val tiltakstypeRepository = TiltakstypeRepository()
    private val tilgangskontrollService: TilgangskontrollService = mockk(relaxed = true)
    private val unleashToggle: UnleashToggle = mockk()

    private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val deltakerRepository = DeltakerRepository()
    private val deltakerService = DeltakerService(
        deltakerRepository = deltakerRepository,
        amtDeltakerClient = mockAmtDeltakerClient(),
        paameldingClient = mockPaameldingClient(),
        navEnhetService = navEnhetService,
        forslagRepository = mockk(relaxed = true),
    )
    private val pameldingService = PameldingService(
        deltakerRepository = deltakerRepository,
        deltakerService = deltakerService,
        navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
        navEnhetService = navEnhetService,
        paameldingClient = mockPaameldingClient(),
    )
}
