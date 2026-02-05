package no.nav.amt.deltaker.bff.deltaker.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.deltaker.bff.DatabaseTestExtension
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerStatus
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import java.time.LocalDate

class DeltakerStatusRepositoryTest {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseTestExtension()
    }

    @Test
    fun `insertIfNotExists - kaster feil hvis flere aktive statuser`() {
        val gammelStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = DeltakerStatus.Aarsak.Type.ANNET,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val deltaker = lagDeltaker(status = gammelStatus)
        TestRepository.insert(deltaker)

        val nyStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = null,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val thrown = shouldThrow<PSQLException> {
            DeltakerStatusRepository.insertIfNotExists(deltaker.id, nyStatus)
        }

        thrown.message shouldStartWith "ERROR: duplicate key value violates unique constraint"
    }

    @Test
    fun `deaktiverTidligereStatuser - skal deaktivere alle andre statuser`() {
        val gammelStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = DeltakerStatus.Aarsak.Type.ANNET,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val nyStatus = lagDeltakerStatus(
            type = DeltakerStatus.Type.HAR_SLUTTET,
            aarsak = null,
            gyldigFra = LocalDate.of(2024, 10, 5).atStartOfDay(),
        )

        val deltaker = lagDeltaker(status = gammelStatus)
        TestRepository.insert(deltaker)

        DeltakerStatusRepository.deaktiverTidligereStatuser(deltaker.id, nyStatus)
        val inaktivStatus = DeltakerStatusRepository.getAktivDeltakerStatus(deltaker.id)
        inaktivStatus.shouldBeNull()

        DeltakerStatusRepository.insertIfNotExists(deltaker.id, nyStatus)
        val aktivStatus = DeltakerStatusRepository.getAktivDeltakerStatus(deltaker.id)
        aktivStatus.shouldNotBeNull()
    }
}
