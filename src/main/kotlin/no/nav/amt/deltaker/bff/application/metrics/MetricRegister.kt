package no.nav.amt.deltaker.bff.application.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "amtdeltakerbff"

object MetricRegister {
    val OPPRETTET_KLADD: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("opprettet_kladd_count")
        .help("Antall opprettede kladder for påmelding")
        .register()

    val DELT_UTKAST: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("delt_utkast_count")
        .help("Antall delte utkast for påmelding")
        .register()

    val PAMELDT_UTEN_UTKAST: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("uten_utkast_count")
        .help("Antall påmeldinger uten utkast")
        .register()
}
