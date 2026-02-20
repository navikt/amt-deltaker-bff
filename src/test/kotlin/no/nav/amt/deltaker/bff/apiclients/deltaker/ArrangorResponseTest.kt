package no.nav.amt.deltaker.bff.apiclients.deltaker

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData.lagArrangorResponse
import org.junit.jupiter.api.Test

class ArrangorResponseTest {
    @Test
    fun `skal mapppe response til model korrekt`() {
        val response = lagArrangorResponse()

        val model = response.toArrangorModel()

        model.navn shouldBe response.navn
    }
}
