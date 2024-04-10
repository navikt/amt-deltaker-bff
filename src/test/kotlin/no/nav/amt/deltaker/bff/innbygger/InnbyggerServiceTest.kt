package no.nav.amt.deltaker.bff.innbygger

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.model.sammenlignVedtak
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test

class InnbyggerServiceTest {
    private val amtDeltakerClient = mockAmtDeltakerClient()
    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val deltakerService = DeltakerService(DeltakerRepository(), amtDeltakerClient, navEnhetService)
    private val innbyggerService = InnbyggerService(amtDeltakerClient, deltakerService)

    companion object {
        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
        }
    }

    @Test
    fun `fattVedtak - deltaker har ikke vedtak som kan fattes - feiler`() {
        val deltaker = TestData.lagDeltaker()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                innbyggerService.fattVedtak(deltaker)
            }
        }
    }

    @Test
    fun `fattVedtak - deltaker har vedtak som kan fattes - kaller amtDeltaker og oppdaterer deltaker`() {
        val deltaker = deltakerMedIkkeFattetVedtak()
        TestRepository.insert(deltaker)

        val deltakerMedFattetVedtak = deltaker.fattVedtak()

        MockResponseHandler.addFattVedtakResponse(deltakerMedFattetVedtak, deltaker.ikkeFattetVedtak!!)

        runBlocking {
            val oppdatertDeltaker = innbyggerService.fattVedtak(deltaker)

            oppdatertDeltaker.ikkeFattetVedtak shouldBe null
            deltaker.ikkeFattetVedtak!!.id shouldBe oppdatertDeltaker.fattetVedtak!!.id

            sammenlignDeltakere(oppdatertDeltaker, deltakerMedFattetVedtak)
            sammenlignVedtak(oppdatertDeltaker.vedtaksinformasjon!!, deltakerMedFattetVedtak.vedtaksinformasjon!!)
        }
    }
}
