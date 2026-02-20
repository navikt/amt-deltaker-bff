package no.nav.amt.deltaker.bff.apiclients.deltaker

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerResponse
import org.junit.jupiter.api.Test

class DeltakerAmtDeltakerResponseTest {
    @Test
    fun `skal mapppe response til model korrekt`() {
        val response = lagDeltakerResponse()

        val model = response.toDeltakerModel()

        assertSoftly(model) {
            id shouldBe response.id
            navBruker shouldBe response.navBruker.toNavBrukerModel()
            gjennomforing shouldBe response.gjennomforing.toGjennomforingModel()
            startdato shouldBe response.startdato.shouldNotBeNull()
            sluttdato shouldBe response.sluttdato.shouldNotBeNull()
            dagerPerUke shouldBe response.dagerPerUke.shouldNotBeNull()
            deltakelsesprosent shouldBe response.deltakelsesprosent.shouldNotBeNull()
            bakgrunnsinformasjon shouldBe response.bakgrunnsinformasjon.shouldNotBeNull()
            deltakelsesinnhold shouldBe response.deltakelsesinnhold.shouldNotBeNull()
            status shouldBe response.status
            kanEndres shouldBe !response.erLaastForEndringer
            sistEndret shouldBe response.sistEndret
            erManueltDeltMedArrangor shouldBe response.erManueltDeltMedArrangor
            erLaastForEndringer shouldBe response.erLaastForEndringer

            response.historikk.shouldNotBeEmpty()
            historikk shouldBe response.historikk

            response.vedtaksinformasjon.shouldNotBeNull()
            vedtaksinformasjon shouldBe response.vedtaksinformasjon.toVedtaksinformasjonModel()

            response.endringsforslagFraArrangor.shouldNotBeEmpty()
            endringsforslagFraArrangor shouldBe response.endringsforslagFraArrangor
        }
    }
}
