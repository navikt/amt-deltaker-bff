package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltakerliste.Mal

data class EndreMalRequest(
    val mal: List<Mal>,
)
