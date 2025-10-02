package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.kafka

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.tiltakskoordinator.extensions.toUlestHendelse
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.UlestHendelseRepository
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.UlestHendelseService
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagHendelse
import no.nav.amt.deltaker.bff.utils.data.TestData.lagTiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.models.hendelse.HendelseType
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HendelseConsumerTest {
    @BeforeEach
    fun cleanDatabase() = TestRepository.cleanDatabase()

    @Test
    fun `consume - hendelse InnbyggerGodkjennUtkast - lagrer`(): Unit = runBlocking {
        val consumer = HendelseConsumer(service)
        val deltakerliste = lagDeltakerliste(
            tiltak = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
        )
        val deltaker = lagDeltaker(deltakerliste = deltakerliste)
        TestRepository.insert(deltaker)

        val hendelse = lagHendelse(
            deltaker = deltaker,
            payload = HendelseType.AvbrytDeltakelse(
                aarsak = null,
                sluttdato = LocalDate.now(),
                begrunnelseFraNav = null,
                begrunnelseFraArrangor = null,
                endringFraForslag = null,
            ),
        )

        consumer.consume(
            hendelse.id,
            objectMapper.writeValueAsString(hendelse),
        )

        val ulestHendelseFraDb = repository.getForDeltaker(deltaker.id)
        ulestHendelseFraDb.size shouldBe 1
        ulestHendelseFraDb.first().id shouldBe hendelse.id
    }

    @Test
    fun `consume - hendelse vi ikke bryr oss om - lagrer ikke`(): Unit = runBlocking {
        val consumer = HendelseConsumer(service)
        val deltakerliste = lagDeltakerliste(
            tiltak = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
        )
        val deltaker = lagDeltaker(deltakerliste = deltakerliste)
        TestRepository.insert(deltaker)
        val hendelse = lagHendelse(
            deltaker = deltaker,
            payload = HendelseType.TildelPlass,
        )

        consumer.consume(
            hendelse.id,
            objectMapper.writeValueAsString(hendelse),
        )

        val ulestHendelseFraDb = repository.get(hendelse.id).getOrNull()
        ulestHendelseFraDb shouldBe null
    }

    @Test
    fun `consume - hendelse tombstone - sletter`(): Unit = runBlocking {
        val consumer = HendelseConsumer(service)
        val deltakerliste = lagDeltakerliste(
            tiltak = lagTiltakstype(tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
        )
        val deltaker = lagDeltaker(deltakerliste = deltakerliste)
        TestRepository.insert(deltaker)
        val hendelse = lagHendelse(deltaker = deltaker)

        hendelse.toUlestHendelse()?.let { repository.upsert(it) }

        consumer.consume(
            hendelse.id,
            null,
        )

        val ulestHendelseFraDb = repository.getForDeltaker(deltaker.id)
        ulestHendelseFraDb.size shouldBe 0
    }

    companion object {
        private lateinit var repository: UlestHendelseRepository
        private lateinit var service: UlestHendelseService

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container
            repository = UlestHendelseRepository()
            service = UlestHendelseService(repository)
        }
    }
}
