package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.data.endre
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import org.junit.Test
import java.time.LocalDate

class DeltakerServiceTest {
    init {
        SingletonPostgresContainer.start()
    }

    private val service = DeltakerService(DeltakerRepository(), mockAmtDeltakerClient())

    @Test
    fun `oppdaterDeltaker - kaller client og returnerer deltaker`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val endringer = listOf(
            DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
            DeltakerEndring.Endring.EndreInnhold(listOf(Innhold("tekst,", "innholdskode,", true, "beskrivelse"))),
            DeltakerEndring.Endring.EndreDeltakelsesmengde(deltakelsesprosent = 50F, dagerPerUke = 2F),
            DeltakerEndring.Endring.EndreStartdato(startdato = LocalDate.now()),
            DeltakerEndring.Endring.EndreSluttdato(sluttdato = LocalDate.now()),
            DeltakerEndring.Endring.EndreSluttarsak(
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
            ),
            DeltakerEndring.Endring.ForlengDeltakelse(LocalDate.now()),
            DeltakerEndring.Endring.IkkeAktuell(
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
            ),
            DeltakerEndring.Endring.AvsluttDeltakelse(
                sluttdato = LocalDate.now(),
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
            ),
        )

        endringer.forEach { endring ->
            MockResponseHandler.addEndringsresponse(
                deltaker.endre(TestData.lagDeltakerEndring(deltakerId = deltaker.id, endring = endring)),
                endring,
            )

            val oppdatertDeltaker = service.oppdaterDeltaker(
                deltaker,
                endring,
                "navIdent",
                "enhetsnummer",
            )

            when (endring) {
                is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> {
                    oppdatertDeltaker.bakgrunnsinformasjon shouldBe endring.bakgrunnsinformasjon
                }

                is DeltakerEndring.Endring.EndreInnhold -> {
                    oppdatertDeltaker.innhold shouldBe endring.innhold
                }

                is DeltakerEndring.Endring.EndreDeltakelsesmengde -> {
                    oppdatertDeltaker.deltakelsesprosent shouldBe endring.deltakelsesprosent
                    oppdatertDeltaker.dagerPerUke shouldBe endring.dagerPerUke
                }

                is DeltakerEndring.Endring.EndreStartdato -> {
                    oppdatertDeltaker.startdato shouldBe endring.startdato
                }

                is DeltakerEndring.Endring.EndreSluttdato -> {
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.EndreSluttarsak -> {
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                }

                is DeltakerEndring.Endring.ForlengDeltakelse -> {
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.IkkeAktuell -> {
                    oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.IKKE_AKTUELL
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                }

                is DeltakerEndring.Endring.AvsluttDeltakelse -> {
                    oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }
            }
        }
    }
}
