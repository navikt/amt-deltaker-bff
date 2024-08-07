package no.nav.amt.deltaker.bff.deltaker.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.Forslag

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DeltakerHistorikk {
    val sistEndret
        get() = when (this) {
            is Endring -> endring.endret
            is Vedtak -> vedtak.sistEndret
            is Forslag -> forslag.sistEndret
        }

    data class Endring(
        val endring: DeltakerEndring,
    ) : DeltakerHistorikk()

    data class Vedtak(
        val vedtak: no.nav.amt.deltaker.bff.deltaker.model.Vedtak,
    ) : DeltakerHistorikk()

    data class Forslag(
        val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag,
    ) : DeltakerHistorikk()

    fun navAnsatte() = when (this) {
        is Endring -> listOf(this.endring.endretAv)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAv, this.vedtak.opprettetAv)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.id)
    }

    fun navEnheter() = when (this) {
        is Endring -> listOf(this.endring.endretAvEnhet)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAvEnhet, this.vedtak.opprettetAvEnhet)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.enhetId)
    }
}

private fun Forslag.getNavAnsatt() = when (val status = this.status) {
    is Forslag.Status.Avvist -> status.avvistAv
    is Forslag.Status.Godkjent -> status.godkjentAv
    is Forslag.Status.Erstattet,
    is Forslag.Status.Tilbakekalt,
    Forslag.Status.VenterPaSvar,
    -> null
}
