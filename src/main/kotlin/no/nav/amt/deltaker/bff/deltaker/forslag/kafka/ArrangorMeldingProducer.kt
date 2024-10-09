package no.nav.amt.deltaker.bff.deltaker.forslag.kafka

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.models.arrangor.melding.Forslag

class ArrangorMeldingProducer(
    private val producer: Producer<String, String>,
) {
    fun produce(forslag: Forslag) {
        producer.produce(Environment.ARRANGOR_MELDING_TOPIC, forslag.id.toString(), objectMapper.writeValueAsString(forslag))
    }
}
