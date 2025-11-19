package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerV2Consumer
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerRepository
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBrukerService
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingRepository
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.unleash.UnleashToggle
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.mockPaameldingClient
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.DeltakerKafkaPayload
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.DeltakerStatusDto
import no.nav.amt.lib.models.deltaker.Deltakerliste
import no.nav.amt.lib.models.deltaker.Kilde
import no.nav.amt.lib.models.deltaker.Kontaktinformasjon
import no.nav.amt.lib.models.deltaker.Navn
import no.nav.amt.lib.models.deltaker.Personalia
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltak
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeltakerV2ConsumerTest {
    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
    }

    private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockAmtPersonServiceClient())
    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val navBrukerService = NavBrukerService(mockAmtPersonServiceClient(), NavBrukerRepository(), navAnsattService, navEnhetService)
    private val deltakerService = DeltakerService(
        DeltakerRepository(),
        mockAmtDeltakerClient(),
        mockPaameldingClient(),
        navEnhetService,
        mockk(relaxed = true),
    )
    private val deltakerlisteRepository = DeltakerlisteRepository()
    private val vurderingService = VurderingService(VurderingRepository())
    private val unleashToggle = mockk<UnleashToggle>()
    private val consumer = DeltakerV2Consumer(deltakerService, deltakerlisteRepository, vurderingService, navBrukerService, unleashToggle)

    @BeforeEach
    fun setup() {
        TestRepository.cleanDatabase()
        every { unleashToggle.erKometMasterForTiltakstype(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING) } returns true
    }

    @Test
    fun `consume - kilde er ARENA, deltaker finnes - konsumerer melding, oppdaterer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste, startdato = null, sluttdato = null)
            TestRepository.insert(deltaker)
            val vurdering = TestData.lagVurdering(deltakerId = deltaker.id)
            val startdato = LocalDate.now().plusDays(1)
            val sluttdato = LocalDate.now().plusWeeks(3)
            val sistEndret = LocalDateTime.now().minusDays(2)
            val mottattDeltaker = deltaker.copy(
                startdato = startdato,
                sluttdato = sluttdato,
                status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
                sistEndret = sistEndret,
            )

            consumer.consume(
                deltaker.id,
                objectMapper.writeValueAsString(mottattDeltaker.toKafkaPayload(Kilde.ARENA, listOf(vurdering), deltakerliste)),
            )

            val oppdatertDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            oppdatertDeltaker.startdato shouldBe startdato
            oppdatertDeltaker.sluttdato shouldBe sluttdato
            oppdatertDeltaker.sistEndret shouldBeCloseTo sistEndret

            val lagretVurdering = vurderingService.getForDeltaker(deltaker.id)
            lagretVurdering.size shouldBe 1
        }
    }

    @Test
    fun `consume - kilde er ARENA, deltaker finnes ikke, ingen andre deltakelser - konsumerer melding, lagrer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(deltaker.toKafkaPayload(Kilde.ARENA, deltakerliste = deltakerliste)),
            )

            val lagretDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
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
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(deltaker.toKafkaPayload(Kilde.ARENA, deltakerliste = deltakerliste)),
            )

            val lagretDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe true

            val lagretTidligereDeltaker = deltakerService.getDeltaker(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe false
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, nyere deltakelse - lagrer, kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(deltaker.toKafkaPayload(Kilde.ARENA, deltakerliste = deltakerliste)),
            )

            val lagretDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe false

            val lagretNyereDeltaker = deltakerService.getDeltaker(nyereDeltakelse.id).getOrThrow()
            lagretNyereDeltaker.kanEndres shouldBe true
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, avsluttet, en avsluttet deltakelse - lagrer, tidligere deltaker kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(deltaker.toKafkaPayload(Kilde.ARENA, deltakerliste = deltakerliste)),
            )

            val lagretDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe true

            val lagretTidligereDeltaker = deltakerService.getDeltaker(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe false
        }
    }

    @Test
    fun `consume - kilde ARENA, finnes ikke, avsluttet, en nyere avsluttet deltakelse - lagrer, eldste deltaker kan ikke endres`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(deltaker.toKafkaPayload(Kilde.ARENA, deltakerliste = deltakerliste)),
            )

            val lagretDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            lagretDeltaker.startdato shouldBe deltaker.startdato
            lagretDeltaker.kanEndres shouldBe false

            val lagretTidligereDeltaker = deltakerService.getDeltaker(tidligereDeltakelse.id).getOrThrow()
            lagretTidligereDeltaker.kanEndres shouldBe true
        }
    }

    @Test
    fun `consume - kilde er KOMET, deltaker finnes - konsumerer melding, oppdaterer`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
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
                objectMapper.writeValueAsString(mottattDeltaker.toKafkaPayload(Kilde.KOMET, deltakerliste = deltakerliste)),
            )

            val oppdatertDeltaker = deltakerService.getDeltaker(deltaker.id).getOrThrow()
            oppdatertDeltaker.startdato shouldBe startdato
            oppdatertDeltaker.sluttdato shouldBe sluttdato
        }
    }

    @Test
    fun `consume - tombstone - sletter deltaker`() {
        runBlocking {
            val deltakerliste = TestData.lagDeltakerliste(
                tiltakstype = TestData.lagTiltakstype(tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            )
            val deltaker = TestData.lagDeltaker(deltakerliste = deltakerliste, startdato = null, sluttdato = null)
            TestRepository.insert(deltaker)

            consumer.consume(deltaker.id, null)

            deltakerService.getDeltaker(deltaker.id).getOrNull() shouldBe null
        }
    }
}

private fun Deltaker.toKafkaPayload(
    kilde: Kilde,
    vurderinger: List<Vurdering> = emptyList(),
    deltakerliste: no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste,
) = DeltakerKafkaPayload(
    id = id,
    deltakerlisteId = deltakerliste.id,
    personalia = Personalia(
        navBruker.personId,
        personident = navBruker.personident,
        navn = Navn(navBruker.fornavn, navBruker.mellomnavn, navBruker.etternavn),
        kontaktinformasjon = Kontaktinformasjon(epost = navBruker.epost, telefonnummer = navBruker.telefon),
        skjermet = navBruker.erSkjermet,
        adresse = navBruker.adresse,
        adressebeskyttelse = navBruker.adressebeskyttelse,
    ),
    status = DeltakerStatusDto(
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
    vurderingerFraArrangor = vurderinger,
    sistEndret = sistEndret,
    deltakerliste = Deltakerliste(
        id = deltakerliste.id,
        navn = deltakerliste.navn,
        tiltak = Tiltak(
            navn = "trallas",
            tiltakskode = deltakerliste.tiltak.tiltakskode,
        ),
        startdato = deltakerliste.startDato,
        sluttdato = deltakerliste.sluttDato,
        oppstartstype = deltakerliste.oppstart,
    ),
    innsoktDato = LocalDate.now(),
    forsteVedtakFattet = LocalDate.now(),
    erManueltDeltMedArrangor = false,
    sisteEndring = null,
    navKontor = null,
    navVeileder = null,
    deltarPaKurs = false,
    oppfolgingsperioder = emptyList(),
    sistEndretAv = null,
    sistEndretAvEnhet = null,
    forcedUpdate = null,
)
