package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.TiltakstypeRepository
import no.nav.amt.deltaker.bff.utils.SingletonPostgresContainer
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import org.junit.BeforeClass
import org.junit.Test

class TiltakstypeConsumerTest {
    companion object {
        lateinit var repository: TiltakstypeRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            repository = TiltakstypeRepository()
            TestRepository.cleanDatabase()
        }
    }

    @Test
    fun `consumeTiltakstype - ny, aktiv tiltakstype - lagrer tiltakstype`() {
        val tiltakstype = TestData.lagTiltakstype()
        val tiltakstypeDto = TiltakstypeDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakstype.tiltakskode,
            arenaKode = tiltakstype.arenaKode.name,
            status = Tiltakstypestatus.Aktiv,
            innsatsgrupper = tiltakstype.innsatsgrupper,
            deltakerRegistreringInnhold = tiltakstype.innhold,
        )
        val consumer = TiltakstypeConsumer(repository)

        runBlocking {
            consumer.consume(
                tiltakstype.id,
                objectMapper.writeValueAsString(tiltakstypeDto),
            )

            repository.get(tiltakstype.arenaKode).getOrThrow() shouldBe tiltakstype
        }
    }
}
