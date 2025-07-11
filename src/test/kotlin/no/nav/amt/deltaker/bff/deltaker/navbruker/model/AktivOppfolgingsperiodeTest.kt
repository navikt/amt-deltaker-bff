package no.nav.amt.deltaker.bff.deltaker.navbruker.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AktivOppfolgingsperiodeTest {
    @Test
    fun `harAktivOppfolgingsperiode - har ingen oppfolgingsperioder - returnerer false`() {
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = emptyList())

        navBruker.harAktivOppfolgingsperiode() shouldBe false
    }

    @Test
    fun `harAktivOppfolgingsperiode - har ikke startet - returnerer false`() {
        val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
            startdato = LocalDateTime.now().plusDays(2),
            sluttdato = null,
        )
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = listOf(oppfolgingsperiode))

        navBruker.harAktivOppfolgingsperiode() shouldBe false
    }

    @Test
    fun `harAktivOppfolgingsperiode - startdato passert, sluttdato null - returnerer true`() {
        val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
            startdato = LocalDateTime.now().minusDays(2),
            sluttdato = null,
        )
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = listOf(oppfolgingsperiode))

        navBruker.harAktivOppfolgingsperiode() shouldBe true
    }

    @Test
    fun `harAktivOppfolgingsperiode - startdato passert, sluttdato om en uke - returnerer true`() {
        val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
            startdato = LocalDateTime.now().minusDays(2),
            sluttdato = LocalDateTime.now().plusWeeks(1),
        )
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = listOf(oppfolgingsperiode))

        navBruker.harAktivOppfolgingsperiode() shouldBe true
    }

    @Test
    fun `harAktivOppfolgingsperiode - startdato passert, sluttdato i dag - returnerer false`() {
        val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
            startdato = LocalDateTime.now().minusYears(1),
            sluttdato = LocalDateTime.now(),
        )
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = listOf(oppfolgingsperiode))

        navBruker.harAktivOppfolgingsperiode() shouldBe false
    }

    @Test
    fun `harAktivOppfolgingsperiode - startdato passert, sluttdato for 2 dager siden - returnerer false`() {
        val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
            startdato = LocalDateTime.now().minusYears(1),
            sluttdato = LocalDateTime.now().minusDays(2),
        )
        val navBruker = TestData.lagNavBruker(oppfolgingsperioder = listOf(oppfolgingsperiode))

        navBruker.harAktivOppfolgingsperiode() shouldBe false
    }
}
