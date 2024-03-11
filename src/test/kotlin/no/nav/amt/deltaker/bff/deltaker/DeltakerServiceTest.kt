package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
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
    fun `oppdaterDeltaker - kaller client og returnerer dum deltaker`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        val endringer = listOf(
            DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
            DeltakerEndring.Endring.EndreInnhold(listOf(Innhold("tekst,", "innholdskode,", true, "beskrivelse"))),
            DeltakerEndring.Endring.EndreDeltakelsesmengde(deltakelsesprosent = 50F, dagerPerUke = 2F),
        )

        endringer.forEach { endring ->
            MockResponseHandler.addEndringsresponse(deltaker.id, endring)

            val oppdatertDeltaker = service.oppdaterDeltaker(
                deltaker,
                endring,
                "navIdent",
                "enhetsnummer",
            )

            when (endring) {
                is DeltakerEndring.Endring.EndreBakgrunnsinformasjon ->
                    oppdatertDeltaker.bakgrunnsinformasjon shouldBe endring.bakgrunnsinformasjon

                is DeltakerEndring.Endring.EndreInnhold ->
                    oppdatertDeltaker.innhold shouldBe endring.innhold

                is DeltakerEndring.Endring.EndreDeltakelsesmengde -> {
                    oppdatertDeltaker.deltakelsesprosent shouldBe endring.deltakelsesprosent
                    oppdatertDeltaker.dagerPerUke shouldBe endring.dagerPerUke
                }

                else -> TODO()
            }
        }
    }
}
