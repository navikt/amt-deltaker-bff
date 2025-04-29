package no.nav.amt.deltaker.bff.deltakerliste.kafka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
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
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtArrangorClient
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

class DeltakerlisteConsumerTest {
    companion object {
        lateinit var repository: DeltakerlisteRepository
        lateinit var tiltakstypeRepository: TiltakstypeRepository
        lateinit var navEnhetService: NavEnhetService
        lateinit var deltakerService: DeltakerService
        lateinit var pameldingService: PameldingService
        lateinit var navAnsattService: NavAnsattService
        lateinit var tilgangskontrollService: TilgangskontrollService

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgres16Container
            repository = DeltakerlisteRepository()
            tiltakstypeRepository = TiltakstypeRepository()
            navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
            navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient(), navEnhetService)
            tilgangskontrollService = mockk(relaxed = true)
            deltakerService = DeltakerService(
                deltakerRepository = DeltakerRepository(),
                amtDeltakerClient = mockAmtDeltakerClient(),
                navEnhetService = navEnhetService,
                forslagService = mockk(relaxed = true),
            )
            pameldingService = PameldingService(
                deltakerService = deltakerService,
                navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService),
                amtDeltakerClient = mockAmtDeltakerClient(),
                navEnhetService = navEnhetService,
            )
        }
    }

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `consumeDeltakerliste - ny liste og arrangor - lagrer deltakerliste`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        TestRepository.insert(tiltakstype = deltakerliste.tiltak)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient(arrangor))
        val consumer = DeltakerlisteConsumer(repository, arrangorService, tiltakstypeRepository, pameldingService, tilgangskontrollService)

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(TestData.lagDeltakerlisteDto(arrangor, deltakerliste)),
            )

            repository.get(deltakerliste.id).getOrThrow() shouldBe deltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - ny sluttdato - oppdaterer deltakerliste`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(repository, arrangorService, tiltakstypeRepository, pameldingService, tilgangskontrollService)

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now())

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(TestData.lagDeltakerlisteDto(arrangor, oppdatertDeltakerliste)),
            )

            repository.get(deltakerliste.id).getOrThrow() shouldBe oppdatertDeltakerliste
        }
    }

    @Test
    fun `consumeDeltakerliste - tombstone - sletter deltakerliste`() {
        val deltakerliste = TestData.lagDeltakerliste()
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())

        TestRepository.insert(deltakerliste)

        val consumer = DeltakerlisteConsumer(repository, arrangorService, tiltakstypeRepository, pameldingService, tilgangskontrollService)

        runBlocking {
            consumer.consume(deltakerliste.id, null)

            repository.get(deltakerliste.id).getOrNull() shouldBe null
        }
    }

    @Test
    fun `consumeDeltakerliste - avbrutt, finnes deltakere - oppdaterer deltakerliste, sletter kladd`() {
        val arrangor = TestData.lagArrangor()
        val deltakerliste = TestData.lagDeltakerliste(arrangor = arrangor)
        val arrangorService = ArrangorService(ArrangorRepository(), mockAmtArrangorClient())
        TestRepository.insert(deltakerliste)
        val kladd = TestData.lagDeltakerKladd(deltakerliste = deltakerliste)
        TestRepository.insert(kladd)
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltaker)
        MockResponseHandler.addSlettKladdResponse(kladd.id)

        val consumer = DeltakerlisteConsumer(repository, arrangorService, tiltakstypeRepository, pameldingService, tilgangskontrollService)

        val oppdatertDeltakerliste = deltakerliste.copy(sluttDato = LocalDate.now(), status = Deltakerliste.Status.AVBRUTT)

        runBlocking {
            consumer.consume(
                deltakerliste.id,
                objectMapper.writeValueAsString(TestData.lagDeltakerlisteDto(arrangor, oppdatertDeltakerliste)),
            )

            repository.get(deltakerliste.id).getOrThrow() shouldBe oppdatertDeltakerliste
            deltakerService.get(kladd.id).getOrNull() shouldBe null
            deltakerService.get(deltaker.id).getOrNull() shouldNotBe null
        }
    }
}
