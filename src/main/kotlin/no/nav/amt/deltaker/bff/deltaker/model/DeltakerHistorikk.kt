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

    data class Endring(val endring: DeltakerEndring) : DeltakerHistorikk()

    data class Vedtak(val vedtak: no.nav.amt.deltaker.bff.deltaker.model.Vedtak) : DeltakerHistorikk()

    data class Forslag(val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag) : DeltakerHistorikk()
}
