package no.nav.amt.deltaker.bff.apiclients.deltaker.request

sealed interface DeltakerEndringRequest {
    val endretAv: String
    val endretAvEnhet: String
}
