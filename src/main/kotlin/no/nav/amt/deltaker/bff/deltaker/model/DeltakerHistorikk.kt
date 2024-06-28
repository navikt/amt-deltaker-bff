package no.nav.amt.deltaker.bff.deltaker.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DeltakerHistorikk.Endring::class, name = "Endring"),
    JsonSubTypes.Type(value = DeltakerHistorikk.Vedtak::class, name = "Vedtak"),
    JsonSubTypes.Type(value = DeltakerHistorikk.Forslag::class, name = "Forslag"),
)
sealed class DeltakerHistorikk {
    val sistEndret
        get() = when (this) {
            is Endring -> endring.endret
            is Vedtak -> vedtak.sistEndret
            is Forslag -> forslag.getSistEndret()
        }

    data class Endring(val endring: DeltakerEndring) : DeltakerHistorikk()

    data class Vedtak(val vedtak: no.nav.amt.deltaker.bff.deltaker.model.Vedtak) : DeltakerHistorikk()

    data class Forslag(val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag) : DeltakerHistorikk()
}

fun Forslag.getSistEndret(): LocalDateTime {
    return when (status) {
        is Forslag.Status.VenterPaSvar -> opprettet
        is Forslag.Status.Avvist -> (status as Forslag.Status.Avvist).avvist
        is Forslag.Status.Godkjent -> (status as Forslag.Status.Godkjent).godkjent
        is Forslag.Status.Tilbakekalt -> (status as Forslag.Status.Tilbakekalt).tilbakekalt
    }
}
