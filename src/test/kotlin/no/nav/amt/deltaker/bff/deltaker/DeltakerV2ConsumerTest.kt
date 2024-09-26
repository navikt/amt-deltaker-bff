package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerV2Consumer
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerV2Dto
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeltakerV2ConsumerTest {
    init {
        SingletonPostgres16Container
    }

    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository())
    private val deltakerService = DeltakerService(DeltakerRepository(), mockAmtDeltakerClient(), navEnhetService)
    private val deltakerlisteRepository = DeltakerlisteRepository()
    private val consumer = DeltakerV2Consumer(deltakerService, deltakerlisteRepository, navBrukerService)

    @Before
    fun setup() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `consume - kilde er ARENA, deltaker finnes - konsumerer melding, oppdaterer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste, startdato = null, sluttdato = null)
            TestRepository.insert(deltaker)

            val startdato = LocalDate.now().plusDays(1)
            val sluttdato = LocalDate.now().plusWeeks(3)
            val mottattDeltaker = deltaker.copy(
                startdato = startdato,
                sluttdato = sluttdato,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(mottattDeltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.startdato shouldBe startdato
            oppdatertDeltaker.sluttdato shouldBe sluttdato
        }
    }

    @Test
    fun `consume - kilde er ARENA, deltaker finnes ikke, ingen andre deltakelser - konsumerer melding, lagrer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            TestRepository.insert(deltakerliste)
            val navbruker = TestData.lagNavBruker()
            val sistEndret = LocalDateTime.now().minusDays(1)
            val statusOpprettet = LocalDateTime.now().minusWeeks(1)
            val deltaker = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                sistEndret = sistEndret,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR, opprettet = statusOpprettet),
            )
            TestRepository.insert(navbruker)
            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val lagretDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe true
            lagretDeltaker.sistEndret shouldBeCloseTo sistEndret
            lagretDeltaker.status.opprettet shouldBeCloseTo statusOpprettet
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, en tidligere deltakelse - lagrer, tidligere deltaker kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val navbruker = TestData.lagNavBruker()
            val tidligereDeltakelse = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            )
            TestRepository.insert(tidligereDeltakelse)

            val deltaker = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val lagretDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe true

            val lagretTidligereDeltaker = deltakerService.get(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe false
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, nyere deltakelse - lagrer, kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val navbruker = TestData.lagNavBruker()
            val nyereDeltakelse = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            )
            TestRepository.insert(nyereDeltakelse)

            val deltaker = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(DeltakerStatus.Type.HAR_SLUTTET),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val lagretDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe false

            val lagretNyereDeltaker = deltakerService.get(nyereDeltakelse.id).getOrThrow()
            lagretNyereDeltaker.kanEndres shouldBe true
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, avsluttet, en avsluttet deltakelse - lagrer, tidligere deltaker kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val navbruker = TestData.lagNavBruker()
            val statusdato = LocalDateTime.now().minusMonths(2)
            val tidligereDeltakelse = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, opprettet = statusdato),
            )
            TestRepository.insert(tidligereDeltakelse)

            val statusdato2 = LocalDateTime.now().minusDays(3)
            val deltaker = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL, opprettet = statusdato2),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val lagretDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe true

            val lagretTidligereDeltaker = deltakerService.get(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe false
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, avsluttet, en nyere avsluttet deltakelse - lagrer, eldste deltaker kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val navbruker = TestData.lagNavBruker()
            val statusdato = LocalDateTime.now().minusDays(2)
            val tidligereDeltakelse = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET, opprettet = statusdato),
            )
            TestRepository.insert(tidligereDeltakelse)

            val statusdato2 = LocalDateTime.now().minusMonths(3)
            val deltaker = TestData.lagDeltaker(
                deltakerliste = deltakerliste,
                navBruker = navbruker,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL, opprettet = statusdato2),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
            )

            val lagretDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe false

            val lagretTidligereDeltaker = deltakerService.get(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe true
        }
    }

    @Test
    fun `consume - kilde er KOMET, deltaker finnes - konsumerer melding, oppdaterer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste, startdato = null, sluttdato = null)
            TestRepository.insert(deltaker)

            val startdato = LocalDate.now().plusDays(1)
            val sluttdato = LocalDate.now().plusWeeks(3)
            val mottattDeltaker = deltaker.copy(
                startdato = startdato,
                sluttdato = sluttdato,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(mottattDeltaker.toV2(DeltakerV2Dto.Kilde.KOMET)),
            )

            val oppdatertDeltaker = deltakerService.get(deltaker.id).getOrThrow()
            oppdatertDeltaker.startdato shouldBe startdato
            oppdatertDeltaker.sluttdato shouldBe sluttdato
        }
    }

    @Test
    fun `consume - tombstone - konsumerer ikke melding`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste, startdato = null, sluttdato = null)
            TestRepository.insert(deltaker)

            consumer.consume(deltaker.id, null)

            deltakerService.get(deltaker.id).getOrNull() shouldNotBe null
        }
    }
}

private fun Deltaker.toV2(kilde: DeltakerV2Dto.Kilde) = DeltakerV2Dto(
    id = id,
    deltakerlisteId = deltakerliste.id,
    personalia = DeltakerV2Dto.DeltakerPersonaliaDto(navBruker.personident),
    status = DeltakerV2Dto.DeltakerStatusDto(
        id = status.id,
        type = status.type,
        aarsak = status.aarsak?.type,
        aarsaksbeskrivelse = status.aarsak?.beskrivelse,
        gyldigFra = status.gyldigFra,
        opprettetDato = status.opprettet,
    ),
    dagerPerUke = dagerPerUke,
    prosentStilling = deltakelsesprosent?.toDouble(),
    oppstartsdato = startdato,
    sluttdato = sluttdato,
    bestillingTekst = bakgrunnsinformasjon,
    kilde = kilde,
    innhold = deltakelsesinnhold,
    historikk = historikk,
    sistEndret = sistEndret,
)
