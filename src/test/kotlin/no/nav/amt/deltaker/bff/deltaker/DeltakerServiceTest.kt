package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
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
    fun `oppdaterDeltaker(endring) - kaller client og returnerer deltaker`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val endringer = listOf(
            DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
            DeltakerEndring.Endring.EndreInnhold(listOf(Innhold("tekst,", "innholdskode,", true, "beskrivelse"))),
            DeltakerEndring.Endring.EndreDeltakelsesmengde(deltakelsesprosent = 50F, dagerPerUke = 2F),
            DeltakerEndring.Endring.EndreStartdato(startdato = LocalDate.now(), sluttdato = LocalDate.now().plusWeeks(2)),
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
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
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

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har ikke andre deltakelser - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            innhold = emptyList(),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har ikke andre deltakelser, har sluttet - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = LocalDate.now().minusMonths(2),
            sluttdato = LocalDate.now().minusDays(2),
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            innhold = emptyList(),
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = DeltakerStatus.Aarsak.Type.ANNET,
                beskrivelse = "Oppdatert",
            ),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
        deltakerFraDb.status.aarsak?.type shouldBe DeltakerStatus.Aarsak.Type.ANNET
        deltakerFraDb.status.aarsak?.beskrivelse shouldBe "Oppdatert"
        deltakerFraDb.kanEndres shouldBe true
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har tidligere deltakelse, statusoppdatering - setter kan ikke endres`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)
        val gammelDeltaker = TestData.lagDeltaker(
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            kanEndres = true,
        )
        TestRepository.insert(gammelDeltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            innhold = emptyList(),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING

        val gammelDeltakerFraDb = service.get(gammelDeltaker.id).getOrThrow()
        gammelDeltakerFraDb.kanEndres shouldBe false
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har tidligere deltakelse, ikke statusoppdatering - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = null,
        )
        TestRepository.insert(deltaker)
        val gammelDeltaker = TestData.lagDeltaker(
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            kanEndres = true,
        )
        TestRepository.insert(gammelDeltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now().plusDays(10),
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            innhold = emptyList(),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.DELTAR

        val gammelDeltakerFraDb = service.get(gammelDeltaker.id).getOrThrow()
        gammelDeltakerFraDb.kanEndres shouldBe true
    }
}
