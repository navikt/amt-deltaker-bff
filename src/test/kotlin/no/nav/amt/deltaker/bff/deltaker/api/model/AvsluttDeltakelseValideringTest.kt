package no.nav.amt.deltaker.bff.deltaker.api.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerStatus
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AvsluttDeltakelseValideringTest {
    @Test
    fun `valider - deltaker er uendret - feiler`() {
        val thrown = shouldThrow<IllegalArgumentException> {
            requestInTest.valider(deltakerInTest)
        }

        thrown.message shouldBe "Kan ikke avslutte deltakelse med uendret sluttdato og årsak"
    }

    @Test
    fun `valider - deltaker har annen sluttdato - ok`() {
        val deltaker = deltakerInTest.copy(sluttdato = yesterday.minusDays(1))

        shouldNotThrowAny {
            requestInTest.valider(deltaker)
        }
    }

    @Test
    fun `valider - deltaker har annen sluttaarsak - ok`() {
        val deltaker = deltakerInTest.copy(sluttdato = yesterday.minusDays(1))

        val request = requestInTest.copy(
            aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT, null),
            sluttdato = yesterday.minusDays(1),
        )

        shouldNotThrowAny {
            request.valider(deltaker)
        }
    }

    companion object {
        private val yesterday: LocalDate = LocalDate.now().minusDays(1)

        private val deltakerInTest = lagDeltaker(
            status = lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
            ),
            sluttdato = yesterday,
        )

        private val requestInTest = AvsluttDeltakelseRequest(
            aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB, null),
            sluttdato = yesterday,
            begrunnelse = "Begrunnelse",
            forslagId = UUID.randomUUID(),
        )
    }
}
