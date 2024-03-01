package no.nav.amt.deltaker.bff.deltaker.model

sealed class DeltakerHistorikk {
    val sistEndret get() = when (this) {
        is Endring -> endring.endret
        is Vedtak -> vedtak.sistEndret
    }

    data class Endring(val endring: DeltakerEndring) : DeltakerHistorikk()
    data class Vedtak(val vedtak: no.nav.amt.deltaker.bff.deltaker.model.Vedtak) : DeltakerHistorikk()
}
