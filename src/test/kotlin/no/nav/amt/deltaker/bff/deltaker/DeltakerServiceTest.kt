package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerEndringRepository
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerEndring
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattRepository
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.mockAmtPersonClient
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeltakerServiceTest {
    companion object {
        lateinit var navAnsatt: NavAnsatt
        lateinit var navEnhet: NavEnhet
        lateinit var deltakerRepository: DeltakerRepository
        lateinit var deltakerService: DeltakerService
        lateinit var deltakerEndringRepository: DeltakerEndringRepository
        lateinit var navAnsattService: NavAnsattService
        lateinit var navEnhetService: NavEnhetService
        lateinit var amtPersonClient: AmtPersonServiceClient

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            navAnsatt = TestData.lagNavAnsatt()
            navEnhet = TestData.lagNavEnhet()
            deltakerRepository = DeltakerRepository()
            deltakerEndringRepository = DeltakerEndringRepository()
            amtPersonClient = mockAmtPersonClient()
            navAnsattService =
                NavAnsattService(NavAnsattRepository(), amtPersonClient)
            navEnhetService =
                NavEnhetService(NavEnhetRepository(), amtPersonClient)
            deltakerService = DeltakerService(
                deltakerRepository,
                deltakerEndringRepository,
                navAnsattService,
                navEnhetService,
            )
        }
    }

    @Before
    fun setupMocks() {
        MockResponseHandler.addNavAnsattResponse(navAnsatt)
        MockResponseHandler.addNavEnhetResponse(navEnhet)
    }

    @Test
    fun `oppdaterDeltaker - oppdatert bakgrunnsinformasjon - oppdaterer i databasen og returnerer oppdatert deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        TestRepository.insert(deltaker)
        val oppdatertBakgrunnsinformasjon = "Oppdatert informasjon"
        val endretAvIdent = navAnsatt.navIdent
        val endretAvEnhetsnummer = navEnhet.enhetsnummer

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                DeltakerEndring.Endring.EndreBakgrunnsinformasjon(oppdatertBakgrunnsinformasjon),
                endretAvIdent,
                endretAvEnhetsnummer,
            )

            oppdatertDeltaker.bakgrunnsinformasjon shouldBe oppdatertBakgrunnsinformasjon
            val endring = deltakerEndringRepository.getForDeltaker(deltaker.id)
            endring.size shouldBe 1
            endring.first().endringstype shouldBe DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON
            endring.first().endring shouldBe DeltakerEndring.Endring.EndreBakgrunnsinformasjon(
                oppdatertBakgrunnsinformasjon,
            )
            endring.first().endret shouldBeCloseTo LocalDateTime.now()
            endring.first().endretAv shouldBe navAnsatt.id
            endring.first().endretAvEnhet shouldBe navEnhet.id
        }
    }

    @Test
    fun `oppdaterDeltaker - oppdatert bakgrunnsinformasjon, men ingen endring - oppdaterer ikke i databasen og returnerer uendret deltaker`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        TestRepository.insert(deltaker)
        val endretAvIdent = navAnsatt.navIdent
        val endretAvEnhetsnummer = navEnhet.enhetsnummer

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.BAKGRUNNSINFORMASJON,
                DeltakerEndring.Endring.EndreBakgrunnsinformasjon(deltaker.bakgrunnsinformasjon),
                endretAvIdent,
                endretAvEnhetsnummer,
            )

            oppdatertDeltaker.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
            val endring = deltakerEndringRepository.getForDeltaker(deltaker.id)
            endring.size shouldBe 0
        }
    }

    @Test
    fun `oppdaterDeltaker - ikke aktuell - oppdaterer deltaker og status`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR))
        TestRepository.insert(deltaker)

        val aarsak = DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.ANNET, "Flyttet til Spania")

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.IKKE_AKTUELL,
                DeltakerEndring.Endring.IkkeAktuell(aarsak),
                navAnsatt.navIdent,
                navEnhet.enhetsnummer,
            )

            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.IKKE_AKTUELL
            oppdatertDeltaker.status.aarsak shouldBe aarsak.toDeltakerStatusAarsak()

            val endring = deltakerEndringRepository.getForDeltaker(deltaker.id)
            endring.size shouldBe 1
            endring[0].endringstype shouldBe DeltakerEndring.Endringstype.IKKE_AKTUELL

            val ikkeAktuellEndring = endring[0].endring as DeltakerEndring.Endring.IkkeAktuell
            ikkeAktuellEndring.aarsak shouldBe aarsak
        }
    }

    @Test
    fun `oppdaterDeltaker - forleng, deltar - oppdaterer deltaker med ny sluttdato`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            sluttdato = LocalDate.now().plusWeeks(1),
        )
        TestRepository.insert(deltaker)

        val nySluttdato = LocalDate.now().plusMonths(1)

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.FORLENGELSE,
                DeltakerEndring.Endring.ForlengDeltakelse(nySluttdato),
                navAnsatt.navIdent,
                navEnhet.enhetsnummer,
            )

            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.DELTAR
            oppdatertDeltaker.sluttdato shouldBe nySluttdato

            val endring = deltakerEndringRepository.getForDeltaker(deltaker.id)
            endring.size shouldBe 1
            endring[0].endringstype shouldBe DeltakerEndring.Endringstype.FORLENGELSE

            val forlengDeltakelseEndring = endring[0].endring as DeltakerEndring.Endring.ForlengDeltakelse
            forlengDeltakelseEndring.sluttdato shouldBe nySluttdato
        }
    }

    @Test
    fun `oppdaterDeltaker - forleng, har sluttet - oppdaterer deltaker med ny sluttdato og status`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
            sluttdato = LocalDate.now().minusWeeks(1),
        )
        TestRepository.insert(deltaker)

        val nySluttdato = LocalDate.now().plusMonths(1)

        runBlocking {
            val oppdatertDeltaker = deltakerService.oppdaterDeltaker(
                deltaker,
                DeltakerEndring.Endringstype.FORLENGELSE,
                DeltakerEndring.Endring.ForlengDeltakelse(nySluttdato),
                navAnsatt.navIdent,
                navEnhet.enhetsnummer,
            )

            oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.DELTAR
            oppdatertDeltaker.sluttdato shouldBe nySluttdato

            val endring = deltakerEndringRepository.getForDeltaker(deltaker.id)
            endring.size shouldBe 1
            endring[0].endringstype shouldBe DeltakerEndring.Endringstype.FORLENGELSE

            val forlengDeltakelseEndring = endring[0].endring as DeltakerEndring.Endring.ForlengDeltakelse
            forlengDeltakelseEndring.sluttdato shouldBe nySluttdato
        }
    }
}
