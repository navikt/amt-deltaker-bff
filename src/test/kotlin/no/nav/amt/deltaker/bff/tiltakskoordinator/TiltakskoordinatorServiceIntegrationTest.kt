package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.DeltakerOppdateringFeilkode
import no.nav.amt.deltaker.bff.deltaker.amtdistribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.toDeltakeroppdateringResponse
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class TiltakskoordinatorServiceIntegrationTest {
    init {
        SingletonPostgres16Container
    }

    private val amtDeltakerClient = mockk<AmtDeltakerClient>()
    private val navEnhetService = mockk<NavEnhetService>()
    private val navAnsattService = mockk<NavAnsattService>()
    private val vurderingService = mockk<VurderingService>()
    private val deltakerService = DeltakerService(DeltakerRepository(), amtDeltakerClient, navEnhetService, mockk<ForslagService>())
    private val amtDistribusjonClient = mockk<AmtDistribusjonClient>()
    private val forslagService = mockk<ForslagService>()
    private val tiltakskoordinatorService = TiltakskoordinatorService(
        amtDeltakerClient,
        deltakerService,
        mockk<TiltakskoordinatorTilgangRepository>(),
        vurderingService,
        navEnhetService,
        navAnsattService,
        amtDistribusjonClient,
        forslagService,
    )

    @Test
    fun `tildelPlass - returnerer og lagrer deltaker med ny status`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val navEnhet = TestData.lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
        val navAnsatt = TestData.lagNavAnsatt(id = deltaker.navBruker.navVeilederId!!)

        TestRepository.insert(deltaker)
        every { vurderingService.getSisteVurderingForDeltaker(deltaker.id) } returns null
        every { navEnhetService.hentEnheter(listOf(navEnhet.id)) } returns mapOf(navEnhet.id to navEnhet)
        every { navAnsattService.hentAnsatte(listOf(navAnsatt.id)) } returns mapOf(navAnsatt.id to navAnsatt)
        coEvery { amtDistribusjonClient.digitalBruker(any()) } returns true
        every { forslagService.getForDeltakere(any()) } returns emptyList()
        every { forslagService.getForDeltaker(any()) } returns emptyList()

        val nyStatus =
            DeltakerStatus(UUID.randomUUID(), DeltakerStatus.Type.VENTER_PA_OPPSTART, null, LocalDateTime.now(), null, LocalDateTime.now())

        coEvery {
            amtDeltakerClient.tildelPlass(listOf(deltaker.id), navAnsatt.navIdent)
        } returns listOf(deltaker.copy(status = nyStatus).toDeltakeroppdateringResponse())

        val resultatFraAmtDeltaker = tiltakskoordinatorService.endreDeltakere(
            listOf(deltaker.id),
            EndringFraTiltakskoordinator.TildelPlass,
            navAnsatt.navIdent,
        )
        val resultDeltaker = resultatFraAmtDeltaker.first()
        resultatFraAmtDeltaker.size shouldBe 1
        resultDeltaker.status.id shouldNotBe deltaker.status.id
        resultDeltaker.status.trimMss().copy(id = nyStatus.id) shouldBe nyStatus.trimMss()

        coEvery { navAnsattService.hentEllerOpprettNavAnsatt(navAnsatt.id) } returns navAnsatt
        coEvery { navEnhetService.hentEnhet(navEnhet.id) } returns navEnhet

        val deltakerFraDb = tiltakskoordinatorService.get(deltaker.id)
        deltakerFraDb shouldBeCloseTo deltaker
            .copy(status = nyStatus)
            .toTiltakskoordinatorsDeltaker(null, navEnhet, navAnsatt, null, false, emptyList())
    }

    @Test
    fun `settPaaVenteliste - returnerer og lagrer deltaker med ny status`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val navEnhet = TestData.lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
        val navAnsatt = TestData.lagNavAnsatt(id = deltaker.navBruker.navVeilederId!!)

        TestRepository.insert(deltaker)
        every { vurderingService.getSisteVurderingForDeltaker(deltaker.id) } returns null
        every { navEnhetService.hentEnheter(listOf(navEnhet.id)) } returns mapOf(navEnhet.id to navEnhet)
        every { navAnsattService.hentAnsatte(listOf(navAnsatt.id)) } returns mapOf(navAnsatt.id to navAnsatt)
        coEvery { amtDistribusjonClient.digitalBruker(any()) } returns true
        every { forslagService.getForDeltakere(any()) } returns emptyList()
        every { forslagService.getForDeltaker(any()) } returns emptyList()

        val nyStatus =
            DeltakerStatus(UUID.randomUUID(), DeltakerStatus.Type.VENTELISTE, null, LocalDateTime.now(), null, LocalDateTime.now())

        coEvery {
            amtDeltakerClient.settPaaVenteliste(listOf(deltaker.id), navAnsatt.navIdent)
        } returns listOf(deltaker.copy(status = nyStatus).toDeltakeroppdateringResponse())

        val resultatFraAmtDeltaker = tiltakskoordinatorService.endreDeltakere(
            listOf(deltaker.id),
            EndringFraTiltakskoordinator.SettPaaVenteliste,
            navAnsatt.navIdent,
        )
        val resultDeltaker = resultatFraAmtDeltaker.first()
        resultatFraAmtDeltaker.size shouldBe 1
        resultDeltaker.status.id shouldNotBe deltaker.status.id
        resultDeltaker.status.trimMss().copy(id = nyStatus.id) shouldBe nyStatus.trimMss()

        coEvery { navAnsattService.hentEllerOpprettNavAnsatt(navAnsatt.id) } returns navAnsatt
        coEvery { navEnhetService.hentEnhet(navEnhet.id) } returns navEnhet

        val deltakerFraDb = tiltakskoordinatorService.get(deltaker.id)
        deltakerFraDb shouldBeCloseTo deltaker
            .copy(status = nyStatus)
            .toTiltakskoordinatorsDeltaker(null, navEnhet, navAnsatt, null, false, emptyList())
    }

    @Test
    fun `settPaaVenteliste - en deltaker feiler i amt-deltaker - returnerer deltaker med feilkode`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val navEnhet = TestData.lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
        val navAnsatt = TestData.lagNavAnsatt(id = deltaker.navBruker.navVeilederId!!)

        TestRepository.insert(deltaker)
        every { vurderingService.getSisteVurderingForDeltaker(deltaker.id) } returns null
        every { navEnhetService.hentEnheter(listOf(navEnhet.id)) } returns mapOf(navEnhet.id to navEnhet)
        every { navAnsattService.hentAnsatte(listOf(navAnsatt.id)) } returns mapOf(navAnsatt.id to navAnsatt)
        every { forslagService.getForDeltakere(any()) } returns emptyList()
        every { forslagService.getForDeltaker(any()) } returns emptyList()

        val nyStatus =
            DeltakerStatus(UUID.randomUUID(), DeltakerStatus.Type.VENTELISTE, null, LocalDateTime.now(), null, LocalDateTime.now())

        coEvery {
            amtDeltakerClient.settPaaVenteliste(listOf(deltaker.id), navAnsatt.navIdent)
        } returns listOf(deltaker.copy(status = nyStatus).toDeltakeroppdateringResponse(feilkode = DeltakerOppdateringFeilkode.UKJENT))

        val resultatFraAmtDeltaker = tiltakskoordinatorService.endreDeltakere(
            listOf(deltaker.id),
            EndringFraTiltakskoordinator.SettPaaVenteliste,
            navAnsatt.navIdent,
        )
        val resultDeltaker = resultatFraAmtDeltaker.first()
        resultatFraAmtDeltaker.size shouldBe 1
        resultatFraAmtDeltaker.first().feilkode shouldBe DeltakerOppdateringFeilkode.UKJENT

        resultDeltaker.status.id shouldNotBe deltaker.status.id
        resultDeltaker.status.trimMss().copy(id = nyStatus.id) shouldBe nyStatus.trimMss()

        coEvery { navAnsattService.hentEllerOpprettNavAnsatt(navAnsatt.id) } returns navAnsatt
        coEvery { navEnhetService.hentEnhet(navEnhet.id) } returns navEnhet

        val deltakerFraDb = tiltakskoordinatorService.get(deltaker.id)
        deltakerFraDb shouldBeCloseTo deltaker
            .copy(status = nyStatus)
            .toTiltakskoordinatorsDeltaker(null, navEnhet, navAnsatt, null, false, emptyList())
    }
}

fun LocalDateTime.atStartOfDay(): LocalDateTime = this.toLocalDate().atStartOfDay()

fun DeltakerStatus.trimMss() = this.copy(
    opprettet = this.opprettet.atStartOfDay(),
    gyldigFra = this.gyldigFra.atStartOfDay(),
)

infix fun TiltakskoordinatorsDeltaker.shouldBeCloseTo(expected: TiltakskoordinatorsDeltaker?) {
    this.copy(
        status = status.trimMss().copy(
            id = expected!!.status.id,
        ),
    ) shouldBe expected.copy(
        status = expected.status.trimMss().copy(
            id = expected.status.id,
        ),
    )
}
