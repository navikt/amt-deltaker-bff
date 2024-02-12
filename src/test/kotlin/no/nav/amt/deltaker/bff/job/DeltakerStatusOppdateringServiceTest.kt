package no.nav.amt.deltaker.bff.job

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerProducer
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import no.nav.amt.deltaker.bff.kafka.utils.SingletonKafkaProvider
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

class DeltakerStatusOppdateringServiceTest {
    companion object {
        lateinit var deltakerRepository: DeltakerRepository
        lateinit var deltakerStatusOppdateringService: DeltakerStatusOppdateringService

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            deltakerRepository = DeltakerRepository()
            deltakerStatusOppdateringService =
                DeltakerStatusOppdateringService(
                    deltakerRepository,
                    DeltakerProducer(
                        LocalKafkaConfig(
                            SingletonKafkaProvider.getHost(),
                        ),
                    ),
                )
        }
    }

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `oppdaterDeltakerStatuser - startdato er passert - setter status DELTAR`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = LocalDate.now().minusDays(1),
            sluttdato = LocalDate.now().plusWeeks(2),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.DELTAR
    }

    @Test
    fun `oppdaterDeltakerStatuser - sluttdato er passert, ikke kurs - setter status HAR_SLUTTET`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusWeeks(1),
            sluttdato = LocalDate.now().minusDays(2),
            deltakerliste = TestData.lagDeltakerliste(
                oppstart = Deltakerliste.Oppstartstype.LOPENDE,
                sluttDato = LocalDate.now().plusMonths(3),
            ),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
    }

    @Test
    fun `oppdaterDeltakerStatuser - sluttdato er passert, kurs - setter status FULLFORT`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusWeeks(1),
            sluttdato = LocalDate.now().minusDays(2),
            deltakerliste = TestData.lagDeltakerliste(
                oppstart = Deltakerliste.Oppstartstype.FELLES,
                sluttDato = LocalDate.now().minusDays(2),
            ),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.FULLFORT
    }

    @Test
    fun `oppdaterDeltakerStatuser - sluttdato er passert og tidligere enn kursets sluttdato - setter status AVBRUTT`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusWeeks(1),
            sluttdato = LocalDate.now().minusDays(2),
            deltakerliste = TestData.lagDeltakerliste(
                oppstart = Deltakerliste.Oppstartstype.FELLES,
                sluttDato = LocalDate.now().plusDays(2),
            ),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.AVBRUTT
    }

    @Test
    fun `oppdaterDeltakerStatuser - deltakerliste avsluttet, status DELTAR - setter status HAR_SLUTTET`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusMonths(1),
            sluttdato = LocalDate.now().plusDays(2),
            deltakerliste = TestData.lagDeltakerliste(
                oppstart = Deltakerliste.Oppstartstype.LOPENDE,
                sluttDato = LocalDate.now().minusDays(2),
                status = Deltakerliste.Status.AVSLUTTET,
            ),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
    }

    @Test
    fun `oppdaterDeltakerStatuser - deltakerliste avsluttet, status VENTER_PA_OPPSTART - setter status IKKE_AKTUELL`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART),
            startdato = null,
            sluttdato = null,
            deltakerliste = TestData.lagDeltakerliste(
                oppstart = Deltakerliste.Oppstartstype.LOPENDE,
                sluttDato = LocalDate.now().minusDays(2),
                status = Deltakerliste.Status.AVSLUTTET,
            ),
        )

        TestRepository.insert(deltaker)

        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()

        val deltakerFraDb = deltakerRepository.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.IKKE_AKTUELL
    }
}
