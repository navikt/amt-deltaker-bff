package no.nav.amt.deltaker.bff.tiltakskoordinator

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Test
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
    private val tiltakskoordinatorService = TiltakskoordinatorService(
        amtDeltakerClient,
        deltakerService,
        mockk<TiltakskoordinatorTilgangRepository>(),
        vurderingService,
        navEnhetService,
        navAnsattService,
    )

    @Test
    fun `settPaaVenteliste - returnerer og lagrer deltaker med ny status`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val navEnhet = TestData.lagNavEnhet(id = deltaker.navBruker.navEnhetId!!)
        val navAnsatt = TestData.lagNavAnsatt(id = deltaker.navBruker.navVeilederId!!)

        TestRepository.insert(deltaker)
        every { vurderingService.getSisteVurderingForDeltaker(deltaker.id) } returns null
        every { navEnhetService.hentEnheter(listOf(navEnhet.id)) } returns mapOf(navEnhet.id to navEnhet)
        every { navAnsattService.hentAnsatte(listOf(navAnsatt.id)) } returns mapOf(navAnsatt.id to navAnsatt)

        val nyStatus =
            DeltakerStatus(UUID.randomUUID(), DeltakerStatus.Type.VENTELISTE, null, LocalDateTime.now(), null, LocalDateTime.now())

        coEvery {
            amtDeltakerClient.settPaaVenteliste(listOf(deltaker.id), deltaker.deltakerliste.id)
        } returns listOf(deltaker.copy(status = nyStatus))

        val resultatFraAmtDeltaker = tiltakskoordinatorService.settPaaVenteliste(listOf(deltaker.id), deltaker.deltakerliste.id)
        val resultDeltaker = resultatFraAmtDeltaker.first()
        resultatFraAmtDeltaker.size shouldBe 1
        resultDeltaker.status shouldBe nyStatus

        coEvery { navAnsattService.hentEllerOpprettNavAnsatt(navAnsatt.id) } returns navAnsatt
        coEvery { navEnhetService.hentEnhet(navEnhet.id) } returns navEnhet

        val deltakerFraDb = tiltakskoordinatorService.get(deltaker.id)
        deltakerFraDb shouldBe deltaker
            .copy(status = nyStatus)
            .toTiltakskoordinatorsDeltaker(null, navEnhet, navAnsatt)
    }
}
