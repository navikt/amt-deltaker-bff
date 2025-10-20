package no.nav.amt.deltaker.bff.unleash

import io.getunleash.Unleash
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.deltaker.bff.unleash.UnleashToggle.Companion.ENABLE_KOMET_DELTAKERE
import no.nav.amt.deltaker.bff.unleash.UnleashToggle.Companion.LES_ARENA_DELTAKERE
import no.nav.amt.deltaker.bff.unleash.UnleashToggle.Companion.LES_GJENNOMFORINGER_V2
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UnleashToggleTest {
    private val unleashClient: Unleash = mockk(relaxed = true)
    private val sut = UnleashToggle(unleashClient)

    @Nested
    inner class ErKometMasterForTiltakstype {
        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ARBEIDSFORBEREDENDE_TRENING",
                "OPPFOLGING",
                "AVKLARING",
                "ARBEIDSRETTET_REHABILITERING",
                "DIGITALT_OPPFOLGINGSTILTAK",
                "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
                "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
                "JOBBKLUBB",
                "GRUPPE_FAG_OG_YRKESOPPLAERING",
            ],
        )
        fun `returnerer true for tiltakstyper som Komet alltid er master for`(kode: Tiltakskode) {
            sut.erKometMasterForTiltakstype(kode.name) shouldBe true
            sut.erKometMasterForTiltakstype(kode) shouldBe true
        }

        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
                "ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
                "HOYERE_UTDANNING",
            ],
        )
        fun `returnerer true hvis toggle ENABLE_KOMET_DELTAKERE er pa for kanskje-master-typer`(kode: Tiltakskode) {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns true

            sut.erKometMasterForTiltakstype(kode.name) shouldBe true
            sut.erKometMasterForTiltakstype(kode) shouldBe true
        }

        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
                "ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
                "HOYERE_UTDANNING",
            ],
        )
        fun `returnerer false hvis toggle ENABLE_KOMET_DELTAKERE er av for kanskje-master-typer`(kode: Tiltakskode) {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns false

            sut.erKometMasterForTiltakstype(kode.name) shouldBe false
            sut.erKometMasterForTiltakstype(kode) shouldBe false
        }
    }

    @Nested
    inner class SkalLeseArenaDataForTiltakstype {
        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
                "ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
                "HOYERE_UTDANNING",
            ],
        )
        fun `returnerer true nar toggle LES_ARENA_DELTAKERE er pa og tiltakstype er lesbar`(kode: Tiltakskode) {
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns true

            sut.skalLeseArenaDataForTiltakstype(kode.name) shouldBe true
            sut.skalLeseArenaDataForTiltakstype(kode) shouldBe true
        }

        @Test
        fun `returnerer false nar toggle LES_ARENA_DELTAKERE er av`() {
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns false

            sut.skalLeseArenaDataForTiltakstype(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING.name) shouldBe false
            sut.skalLeseArenaDataForTiltakstype(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING) shouldBe false
        }

        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ARBEIDSFORBEREDENDE_TRENING",
                "OPPFOLGING",
                "AVKLARING",
                "ARBEIDSRETTET_REHABILITERING",
                "DIGITALT_OPPFOLGINGSTILTAK",
                "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
                "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
                "JOBBKLUBB",
                "GRUPPE_FAG_OG_YRKESOPPLAERING",
            ],
        )
        fun `returnerer false for tiltakstyper som ikke er lesbare selv om toggle er pa`(kode: Tiltakskode) {
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns true

            sut.skalLeseArenaDataForTiltakstype(kode.name) shouldBe false
            sut.skalLeseArenaDataForTiltakstype(kode) shouldBe false
        }
    }

    @Nested
    inner class SkalLeseGjennomforingerV2 {
        @Test
        fun `returnerer true nar toggle LES_GJENNOMFORINGER_V2 er pa`() {
            every { unleashClient.isEnabled(LES_GJENNOMFORINGER_V2) } returns true
            sut.skalLeseGjennomforingerV2() shouldBe true
        }

        @Test
        fun `returnerer false nar toggle LES_GJENNOMFORINGER_V2 er av`() {
            every { unleashClient.isEnabled(LES_GJENNOMFORINGER_V2) } returns false
            sut.skalLeseGjennomforingerV2() shouldBe false
        }
    }

    @Nested
    inner class SkipProsesseringAvGjennomforing {
        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ARBEIDSFORBEREDENDE_TRENING",
                "OPPFOLGING",
                "AVKLARING",
                "ARBEIDSRETTET_REHABILITERING",
                "DIGITALT_OPPFOLGINGSTILTAK",
                "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
                "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
                "JOBBKLUBB",
                "GRUPPE_FAG_OG_YRKESOPPLAERING",
            ],
        )
        fun `returnerer false for tiltakskoder Komet er master for`(tiltakskode: Tiltakskode) {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns true
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns false

            sut.skipProsesseringAvGjennomforing(tiltakskode.name) shouldBe false
        }

        @ParameterizedTest
        @EnumSource(
            value = Tiltakskode::class,
            names = [
                "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
                "ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
                "HOYERE_UTDANNING",
            ],
        )
        fun `returnerer false for enkeltplass tiltakskoder `(tiltakskode: Tiltakskode) {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns false
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns true

            sut.skipProsesseringAvGjennomforing(tiltakskode.name) shouldBe false
        }

        @Test
        fun `returnerer true for tiltakskoder som ikke skal prosesseres`() {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns false
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns true

            sut.skipProsesseringAvGjennomforing("~tiltakskode~") shouldBe true
        }

        @Test
        fun `returnerer true for tiltakskoder som ikke skal prosesseres #2`() {
            every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns true
            every { unleashClient.isEnabled(LES_ARENA_DELTAKERE) } returns false

            sut.skipProsesseringAvGjennomforing("~tiltakskode~") shouldBe true
        }
    }
}
