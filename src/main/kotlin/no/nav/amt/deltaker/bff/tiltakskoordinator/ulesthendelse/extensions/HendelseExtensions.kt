package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.extensions

import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.UlestHendelse
import no.nav.amt.lib.models.hendelse.Hendelse

fun Hendelse.toUlestHendelse() = this.payload.toUlestHendelseType()?.let {
    UlestHendelse(
        this.id,
        this.opprettet,
        this.deltaker.id,
        this.ansvarlig,
        it,
    )
}
