package no.nav.amt.deltaker.bff.application.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

const val METRICS_NS = "amtdeltakerbff"

class MetricRegister(
    collectorRegistry: CollectorRegistry,
) {
    private val opprettetKladd: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("opprettet_kladd_count")
        .help("Antall opprettede kladder for påmelding")
        .register(collectorRegistry)

    fun incOpprettetKladd() {
        opprettetKladd.inc()
    }
}
