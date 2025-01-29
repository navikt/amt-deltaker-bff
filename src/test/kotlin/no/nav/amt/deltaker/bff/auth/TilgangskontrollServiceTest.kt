package no.nav.amt.deltaker.bff.auth

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.junit.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class TilgangskontrollServiceTest {
    private val poaoTilgangCachedClient = mockk<PoaoTilgangCachedClient>()

    private val navAnsattService = NavAnsattService(NavAnsattRepository(), mockk())
    private val tiltakskoordinatorTilgangRepository = TiltakskoordinatorTilgangRepository()
    private val tilgangskontrollService = TilgangskontrollService(
        poaoTilgangCachedClient,
        navAnsattService,
        tiltakskoordinatorTilgangRepository,
    )

    init {
        SingletonPostgres16Container
    }

    @Test
    fun `verifiserSkrivetilgang - har tilgang - kaster ingen feil`() {
        every { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)

        tilgangskontrollService.verifiserSkrivetilgang(UUID.randomUUID(), "12345")
    }

    @Test
    fun `verifiserSkrivetilgang - har ikke tilgang - kaster AuthorizationException`() {
        every { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))

        assertFailsWith<AuthorizationException> {
            tilgangskontrollService.verifiserSkrivetilgang(UUID.randomUUID(), "12345")
        }
    }

    @Test
    fun `verifiserLesetilgang - har tilgang - kaster ingen feil`() {
        every { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Permit)

        tilgangskontrollService.verifiserLesetilgang(UUID.randomUUID(), "12345")
    }

    @Test
    fun `verifiserLesetilgang - har ikke tilgang - kaster AuthorizationException`() {
        every { poaoTilgangCachedClient.evaluatePolicy(any()) } returns ApiResult(null, Decision.Deny("Ikke tilgang", ""))

        assertFailsWith<AuthorizationException> {
            tilgangskontrollService.verifiserLesetilgang(UUID.randomUUID(), "12345")
        }
    }

    @Test
    fun `leggTilTiltakskoordinatorTilgang - har ikke tilgang fra før - returnerer sucess`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            val resultat = tilgangskontrollService.leggTilTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            resultat.isSuccess shouldBe true
        }
    }

    @Test
    fun `leggTilTiltakskoordinatorTilgang - med inaktiv tilgang fra før - returnerer sucess`(): Unit = runBlocking {
        with(TiltakskoordinatorTilgangContext()) {
            medInaktivTilgang()
            val resultat = tilgangskontrollService.leggTilTiltakskoordinatorTilgang(navAnsatt.navIdent, deltakerliste.id)
            resultat.isSuccess shouldBe true
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
}
