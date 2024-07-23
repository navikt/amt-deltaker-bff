package no.nav.amt.deltaker.bff.deltaker.api.model

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import java.util.UUID

sealed interface Endringsrequest {
    fun valider(deltaker: Deltaker)
}

sealed interface EndringsforslagRequest : Endringsrequest {
    val forslagId: UUID?
}
