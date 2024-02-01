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

    private val deltUtkast: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("delt_utkast_count")
        .help("Antall delte utkast for påmelding")
        .register(collectorRegistry)

    private val pameldtUtenUtkast: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("uten_utkast_count")
        .help("Antall påmeldinger uten utkast")
        .register(collectorRegistry)

    fun incOpprettetKladd() {
        opprettetKladd.inc()
    }

    fun incDeltUtkast() {
        deltUtkast.inc()
    }

    fun incPameldtUtenUtkast() {
        pameldtUtenUtkast.inc()
    }
}
