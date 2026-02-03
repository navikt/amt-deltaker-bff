package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.DatabaseTestExtension
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerStatus
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

class DeltakerStatusRepositoryTest {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseTestExtension()
    }

    @Test
    fun `deaktiverUkritiskTidligereStatuserQuery - skal deaktivere alle andre statuser`() {
        val gammelStatus1 = lagDeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            gyldigFra = LocalDate.of(2024, 7, 14).atStartOfDay(),
            gyldigTil = LocalDate.of(2024, 10, 9).atStartOfDay(),
        )
        val gammelStatus2 = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = DeltakerStatus.Aarsak.Type.ANNET,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val nyStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = null,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val deltaker = lagDeltaker(status = nyStatus)
        TestRepository.insert(deltaker)

        TestRepository.insert(gammelStatus1, deltaker.id)
        TestRepository.insert(gammelStatus2, deltaker.id)

        DeltakerStatusRepository.deaktiverUkritiskTidligereStatuserQuery(nyStatus, deltaker.id)

        val statuser = DeltakerStatusRepository.getDeltakerStatuser(deltaker.id)
        statuser.size shouldBe 3
        statuser.filter { it.gyldigTil == null }.size shouldBe 1
        statuser.first { it.gyldigTil == null }.id shouldBe nyStatus.id
    }

    @Test
    fun `getDeltakereMedFlereGyldigeStatuser - deltaker har flere statuser som er gyldig - returnerer statuser`() {
        val deltakerInTest = lagDeltaker(
            status = lagDeltakerStatus(DeltakerStatus.Type.DELTAR),
        )
        TestRepository.insert(deltakerInTest)

        val oppdatertStatus: DeltakerStatus = lagDeltakerStatus(DeltakerStatus.Type.FULLFORT)
        TestRepository.insert(oppdatertStatus, deltakerInTest.id)

        DeltakerStatusRepository.getDeltakereMedFlereGyldigeStatuser().shouldNotBeEmpty()
    }
}
