package no.nav.amt.deltaker.bff.auth

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteService
import no.nav.amt.deltaker.bff.kafka.utils.assertProduced
import no.nav.amt.deltaker.bff.kafka.utils.assertProducedTombstone
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.tiltakskoordinator.TiltakskoordinatorService
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattBehandleFortroligBrukerePolicyInput
import no.nav.poao_tilgang.client.NavAnsattBehandleSkjermedePersonerPolicyInput
import no.nav.poao_tilgang.client.NavAnsattBehandleStrengtFortroligBrukerePolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PolicyInput
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertFailsWith

class TilgangskontrollServiceTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()

    private val kafkaProducer = Producer<String, String>(LocalKafkaConfig(SingletonKafkaProvider.getHost()))
    private val tiltakskoordinatorsDeltakerlisteProducer = TiltakskoordinatorsDeltakerlisteProducer(kafkaProducer)

    private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockk())
    private val tiltakskoordinatorTilgangRepository = TiltakskoordinatorTilgangRepository()
    private val tilgangskontrollService = TilgangskontrollService(
        poaoTilgangCachedClient,
        navAnsattService,
        tiltakskoordinatorTilgangRepository,
        tiltakskoordinatorsDeltakerlisteProducer,
        mockk<TiltakskoordinatorService>(),
        mockk<DeltakerlisteService>(),
    )

    init {
        SingletonPostgres16Container
    }

    @Test
    fun `verifiserSkrivetilgang - har tilgang - kaster ingen feil`() {
        mockPoaoTilgangPermit()

        tilgangskontrollService.verifiserSkrivetilgang(UUID.randomUUID(), "12345")
    }

    @Test
    fun `verifiserSkrivetilgang - har ikke tilgang - kaster AuthorizationException`() {
        mockPoaoTilgangDeny()

        assertFailsWith<AuthorizationException> {
            tilgangskontrollService.verifiserSkrivetilgang(UUID.randomUUID(), "12345")
        }
    }

    @Test
    fun `verifiserLesetilgang - har tilgang - kaster ingen feil`() {
        mockPoaoTilgangPermit()

        tilgangskontrollService.verifiserLesetilgang(UUID.randomUUID(), "12345")
    }

    @Test
    fun `verifiserLesetilgang - har ikke tilgang - kaster AuthorizationException`() {
        mockPoaoTilgangDeny()

        assertFailsWith<AuthorizationException> {
            tilgangskontrollService.verifiserLesetilgang(UUID.randomUUID(), "12345")
        }
    }

    @Test
    fun `leggTilTiltakskoordinatorTilgang - har ikke tilgang fra før - returnerer success`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            val resultat = tilgangskontrollService.leggTilTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            resultat.isSuccess shouldBe true
            assertProduced(resultat.getOrThrow().toDto(navAnsatt.navIdent))
        }
    }

    @Test
    fun `leggTilTiltakskoordinatorTilgang - med inaktiv tilgang fra før - returnerer success`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            val resultat = tilgangskontrollService.leggTilTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            resultat.isSuccess shouldBe true
            assertProduced(resultat.getOrThrow().toDto(navAnsatt.navIdent))
        }
    }

    @Test
    fun `leggTilTiltakskoordinatorTilgang - har tilgang fra før - returnerer failure`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            val resultat = tilgangskontrollService.leggTilTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            resultat.isFailure shouldBe true
        }
    }

    @Test
    fun `verifiserTiltakskoordinatorTilgang - har tilgang - kaster ikke exception`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
        }
    }

    @Test
    fun `verifiserTiltakskoordinatorTilgang - har ingen tilgang - kaster exception`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            assertThrows<AuthorizationException> {
                tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            }
        }
    }

    @Test
    fun `verifiserTiltakskoordinatorTilgang - har inaktiv tilgang - kaster exception`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            assertThrows<AuthorizationException> {
                tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            }
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - mangler tilgang - deltaker er kode 7 - tilgang er false`() {
        with(TiltakskoordinatorTilgangContext()) {
            medFortroligDeltaker()
            mockPoaoTilgangDeny(NavAnsattBehandleFortroligBrukerePolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe false
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - mangler tilgang - deltaker er kode 6 - tilgang er false`() {
        with(TiltakskoordinatorTilgangContext()) {
            medStrengtFortroligDeltaker()
            mockPoaoTilgangDeny(NavAnsattBehandleStrengtFortroligBrukerePolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe false
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - har tilgang - deltaker er kode 7 - tilgang er true`() {
        with(TiltakskoordinatorTilgangContext()) {
            medFortroligDeltaker()
            mockPoaoTilgangPermit(NavAnsattBehandleFortroligBrukerePolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe true
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - har tilgang - deltaker er kode 6 - tilgang er true`() {
        with(TiltakskoordinatorTilgangContext()) {
            medStrengtFortroligDeltaker()
            mockPoaoTilgangPermit(NavAnsattBehandleStrengtFortroligBrukerePolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe true
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - har tilgang - deltaker er skjermet - tilgang er true`() {
        with(TiltakskoordinatorTilgangContext()) {
            medSkjermetDeltaker()
            mockPoaoTilgangPermit(NavAnsattBehandleSkjermedePersonerPolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe true
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - har ikke tilgang - deltaker er skjermet - tilgang er false`() {
        with(TiltakskoordinatorTilgangContext()) {
            medSkjermetDeltaker()
            mockPoaoTilgangDeny(NavAnsattBehandleSkjermedePersonerPolicyInput(navAnsattAzureId))
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe false
        }
    }

    @Test
    fun `koordinatorTilgangTilDeltaker - deltaker er ikke adressebeskyttet eller skjermet - tilgang er true`() {
        with(TiltakskoordinatorTilgangContext()) {
            val tilgangTilDeltaker = tilgangskontrollService.harKoordinatorTilgangTilDeltaker(navAnsattAzureId, deltaker)
            tilgangTilDeltaker shouldBe true
        }
    }

    @Test
    fun `stengTiltakskoordinatorTilgang - aktiv tilgang - tilgang stenges`() {
        with(TiltakskoordinatorTilgangContext()) {
            medAktivTilgang()
            val stengtTilgang = tilgangskontrollService.stengTiltakskoordinatorTilgang(tilgang.id)

            runBlocking {
                assertThrows<AuthorizationException> {
                    tilgangskontrollService.verifiserTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
                }
            }

            assertProducedTombstone(stengtTilgang.getOrThrow())
        }
    }

    @Test
    fun `stengTiltakskoordinatorTilgang - ikke aktiv tilgang - tilgang stenges ikke på nytt`() {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            val resultat = tilgangskontrollService.stengTiltakskoordinatorTilgang(tilgang.id)

            resultat.isFailure shouldBe true
        }
    }

    private fun mockPoaoTilgangDeny(policyInput: PolicyInput? = null) {
        every { poaoTilgangCachedClient.evaluatePolicy(policyInput ?: any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))
    }

    private fun mockPoaoTilgangPermit(policyInput: PolicyInput? = null) {
        every { poaoTilgangCachedClient.evaluatePolicy(policyInput ?: any()) } returns ApiResult(null, Decision.Permit)
    }
}
