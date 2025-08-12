package no.nav.amt.deltaker.bff.deltaker.forslag.kafka

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.models.arrangor.melding.Melding
import no.nav.amt.lib.utils.objectMapper

class ArrangorMeldingProducer(
    private val producer: Producer<String, String>,
) {
    fun produce(melding: Melding) {
        producer.produce(Environment.ARRANGOR_MELDING_TOPIC, melding.id.toString(), objectMapper.writeValueAsString(melding))
    }
}
