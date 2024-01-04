package no.nav.amt.deltaker.bff.deltaker.kafka

import no.nav.amt.deltaker.bff.kafka.config.LocalKafkaConfig
import no.nav.amt.deltaker.bff.kafka.utils.SingletonKafkaProvider
import no.nav.amt.deltaker.bff.kafka.utils.assertProduced
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test

class DeltakerProducerTest {
    @Test
    fun `produce - deltaker - melding produseres`() {
        val deltaker = TestData.lagDeltaker()
        val producer = DeltakerProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost()))
        producer.produce(deltaker)

        assertProduced(deltaker)
    }
}
