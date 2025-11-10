package no.nav.amt.deltaker.bff.deltakerliste.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.schema.shouldMatchSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.kafka.DeltakerlistePayloadJsonSchemas.deltakerlistePayloadV2Schema
import no.nav.amt.deltaker.bff.utils.data.TestData.lagArrangor
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakstype
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DeltakerlistePayloadTest {
    @Nested
    inner class Tiltakskodenavn {
        @Test
        fun `kaster feil hvis tiltakskode mangler`() {
            val payload = DeltakerlistePayload(
                id = deltakerlisteIdInTest,
                arrangor = arrangorDtoInTest,
            )

            shouldThrow<IllegalStateException> {
                payload.effectiveTiltakskode
            }
        }

        @Test
        fun `returnerer tiltakskode fra Tiltakstype`() {
            val expectedTiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING.name

            val payload = DeltakerlistePayload(
                id = deltakerlisteIdInTest,
                tiltakstype = DeltakerlistePayload.Tiltakstype(expectedTiltakskode),
                arrangor = arrangorDtoInTest,
            )

            payload.effectiveTiltakskode shouldBe expectedTiltakskode
        }

        @Test
        fun `returnerer tiltakskode fra tiltakskode`() {
            val expectedTiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name

            val payload = DeltakerlistePayload(
                id = deltakerlisteIdInTest,
                tiltakskode = expectedTiltakskode,
                arrangor = arrangorDtoInTest,
            )

            payload.effectiveTiltakskode shouldBe expectedTiltakskode
        }
    }

    @Nested
    inner class ToModel {
        @Test
        fun `toModel - mapper felter korrekt`() {
            val payload = fullyPopulatedV2PayloadInTest.copy()

            val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING)

            val model = payload.toModel(arrangorInTest, tiltakstype)

            assertSoftly(model) {
                it.tiltak shouldBe tiltakstype
                it.arrangor.arrangor shouldBe arrangorInTest

                id shouldBe id
                navn shouldBe "Testliste"
                startDato shouldBe LocalDate.of(2024, 1, 1)
                sluttDato shouldBe LocalDate.of(2024, 6, 1)
                status shouldBe Deltakerliste.Status.GJENNOMFORES
                oppstart shouldBe Oppstartstype.LOPENDE
                apentForPamelding.shouldBeTrue()
                antallPlasser shouldBe 42
            }
        }

        @Test
        fun `toModel - bruker tiltakstype-navn hvis navn er null`() {
            val payload = DeltakerlistePayload(
                id = deltakerlisteIdInTest,
                tiltakstype = DeltakerlistePayload.Tiltakstype(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING.name),
                arrangor = arrangorDtoInTest,
            )

            val tiltakstype = lagTiltakstype(tiltakskode = Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING)

            val model = payload.toModel(arrangorInTest, tiltakstype)

            model.navn shouldBe "Test tiltak ENKFAGYRKE"
            model.status shouldBe null
        }
    }

    @Nested
    inner class Validate {
        @Test
        fun `fullt populert V2 skal matche skjema`() {
            val json = objectMapper
                .copy()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(fullyPopulatedV2PayloadInTest.copy())

            json.shouldMatchSchema(deltakerlistePayloadV2Schema)
        }
    }

    companion object {
        private val deltakerlisteIdInTest = UUID.randomUUID()

        private val arrangorInTest = lagArrangor()
        private val arrangorDtoInTest = DeltakerlistePayload.Arrangor(arrangorInTest.organisasjonsnummer)

        private val fullyPopulatedV2PayloadInTest = DeltakerlistePayload(
            type = DeltakerlistePayload.ENKELTPLASS_V2_TYPE,
            id = deltakerlisteIdInTest,
            tiltakstype = DeltakerlistePayload.Tiltakstype(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING.name),
            navn = "Testliste",
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2024, 6, 1),
            status = "GJENNOMFORES",
            oppstart = Oppstartstype.LOPENDE,
            apentForPamelding = true,
            arrangor = arrangorDtoInTest,
            antallPlasser = 42,
            oppmoteSted = "~oppmoteSted~",
        )
    }
}
