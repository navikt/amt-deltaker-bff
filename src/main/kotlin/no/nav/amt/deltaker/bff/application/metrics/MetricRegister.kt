package no.nav.amt.deltaker.bff.application.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "amtdeltakerbff"

val OPPRETTET_KLADD = Counter.build()
    .namespace(METRICS_NS)
    .name("opprettet_kladd_count")
    .help("Antall opprettede kladder for påmelding")
    .register()
