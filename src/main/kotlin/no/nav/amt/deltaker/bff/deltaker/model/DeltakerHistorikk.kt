package no.nav.amt.deltaker.bff.deltaker.model

sealed class DeltakerHistorikk {
    data class Endring(val endring: DeltakerEndring) : DeltakerHistorikk()
    data class Vedtak(val vedtak: Vedtak) : DeltakerHistorikk()
}
