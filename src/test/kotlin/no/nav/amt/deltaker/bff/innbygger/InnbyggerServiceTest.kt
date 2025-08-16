package no.nav.amt.deltaker.bff.innbygger

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.db.sammenlignDeltakere
import no.nav.amt.deltaker.bff.deltaker.model.sammenlignVedtak
import no.nav.amt.deltaker.bff.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.mockPaameldingClient
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class InnbyggerServiceTest {
    private val amtDeltakerClient = mockAmtDeltakerClient()
    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val deltakerService = DeltakerService(DeltakerRepository(), amtDeltakerClient, mockPaameldingClient(), navEnhetService, mockk())
    private val innbyggerService = InnbyggerService(deltakerService, mockPaameldingClient())

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container
        }
    }

    @Test
    fun `godkjennUtkast - har feil status - feiler`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                innbyggerService.godkjennUtkast(deltaker)
            }
        }
    }

    @Test
    fun `godkjennUtkast - har riktig status - kaller amtDeltaker og oppdaterer deltaker`() {
        val deltaker = deltakerMedIkkeFattetVedtak()
        TestRepository.insert(deltaker)

        val deltakerMedFattetVedtak = deltaker.fattVedtak()

        MockResponseHandler.addInnbyggerGodkjennUtkastResponse(deltakerMedFattetVedtak)

        runBlocking {
            val oppdatertDeltaker = innbyggerService.godkjennUtkast(deltaker)

            oppdatertDeltaker.ikkeFattetVedtak shouldBe null
            deltaker.ikkeFattetVedtak!!.id shouldBe oppdatertDeltaker.fattetVedtak!!.id

            sammenlignDeltakere(oppdatertDeltaker, deltakerMedFattetVedtak)
            sammenlignVedtak(oppdatertDeltaker.vedtaksinformasjon!!, deltakerMedFattetVedtak.vedtaksinformasjon!!)
        }
    }
}
