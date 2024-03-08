package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import org.junit.Test

class DeltakerServiceTest {
    init {
        SingletonPostgresContainer.start()
    }

    private val service = DeltakerService(DeltakerRepository(), mockAmtDeltakerClient())

    @Test
    fun `oppdaterDeltaker - bakgrunnsinformasjon - kaller client og returnerer dum deltaker`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo")

        MockResponseHandler.addEndringsresponse(deltaker.id, endring)

        val oppdatertDeltaker = service.oppdaterDeltaker(
            deltaker,
            endring,
            "navIdent",
            "enhetsnummer",
        )

        oppdatertDeltaker.bakgrunnsinformasjon shouldBe endring.bakgrunnsinformasjon
    }
}
