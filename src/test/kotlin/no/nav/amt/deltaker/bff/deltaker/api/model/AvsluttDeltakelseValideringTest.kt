package no.nav.amt.deltaker.bff.deltaker.api.model

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class AvsluttDeltakelseValideringTest {
    @Test
    fun `valider - deltaker er uendret - feiler`() {
        shouldThrow<IllegalArgumentException> {
            val deltaker = TestData.lagDeltaker(
                status = TestData.lagDeltakerStatus(
                    type = DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
                ),
                sluttdato = LocalDate.now().minusDays(1),
            )
            val request = AvsluttDeltakelseRequest(
                aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB, null),
                sluttdato = LocalDate.now().minusDays(1),
                begrunnelse = "Begrunnelse",
                forslagId = UUID.randomUUID(),
            )

            request.valider(deltaker)
        }
    }

    @Test
    fun `valider - deltaker har annen sluttdato - ok`() {
        shouldNotThrow<IllegalArgumentException> {
            val deltaker = TestData.lagDeltaker(
                status = TestData.lagDeltakerStatus(
                    type = DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
                ),
                sluttdato = LocalDate.now().minusDays(2),
            )
            val request = AvsluttDeltakelseRequest(
                aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.FATT_JOBB, null),
                sluttdato = LocalDate.now().minusDays(1),
                begrunnelse = "Begrunnelse",
                forslagId = UUID.randomUUID(),
            )

            request.valider(deltaker)
        }
    }

    @Test
    fun `valider - deltaker har annen sluttaarsak - ok`() {
        shouldNotThrow<IllegalArgumentException> {
            val deltaker = TestData.lagDeltaker(
                status = TestData.lagDeltakerStatus(
                    type = DeltakerStatus.Type.HAR_SLUTTET,
                    aarsak = DeltakerStatus.Aarsak.Type.FATT_JOBB,
                ),
                sluttdato = LocalDate.now().minusDays(2),
            )
            val request = AvsluttDeltakelseRequest(
                aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.IKKE_MOTT, null),
                sluttdato = LocalDate.now().minusDays(2),
                begrunnelse = "Begrunnelse",
                forslagId = UUID.randomUUID(),
            )

            request.valider(deltaker)
        }
    }
}
