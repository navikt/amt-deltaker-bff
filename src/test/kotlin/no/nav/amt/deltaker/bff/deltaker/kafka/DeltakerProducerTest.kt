package no.nav.amt.deltaker.bff.deltaker.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import no.nav.amt.deltaker.bff.kafka.utils.SingletonKafkaProvider
import no.nav.amt.deltaker.bff.kafka.utils.stringStringConsumer
import no.nav.amt.deltaker.bff.utils.AsyncUtils
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test
import java.util.UUID

class DeltakerProducerTest {
    @Test
    fun `produce - deltaker - melding produseres`() {
        val deltaker = TestData.lagDeltaker()
        val producer = DeltakerProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost()))
        producer.produce(deltaker)

        AsyncUtils.eventually {
            stringStringConsumer(Environment.DELTAKER_ENDRING_TOPIC) { k, v ->
                UUID.fromString(k) shouldBe deltaker.id
                val dto = objectMapper.readValue<DeltakerDto>(v)
                dto shouldBe deltaker.toDto()
            }.run()
        }
    }
}
