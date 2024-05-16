package no.nav.amt.deltaker.bff.application.metrics

import io.prometheus.metrics.core.metrics.Counter

const val METRICS_NS = "amtdeltakerbff"

object MetricRegister {
    val OPPRETTET_KLADD: Counter = Counter.builder()
        .name("${METRICS_NS}_opprettet_kladd_count")
        .help("Antall opprettede kladder for påmelding")
        .withoutExemplars()
        .register()

    val DELT_UTKAST: Counter = Counter.builder()
        .name("${METRICS_NS}_delt_utkast_count")
        .help("Antall delte utkast for påmelding")
        .withoutExemplars()
        .register()

    val PAMELDT_UTEN_UTKAST: Counter = Counter.builder()
        .name("${METRICS_NS}_uten_utkast_count")
        .help("Antall påmeldinger uten utkast")
        .withoutExemplars()
        .register()
}
