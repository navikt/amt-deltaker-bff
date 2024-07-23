package no.nav.amt.deltaker.bff.deltaker.api.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Endringsrequest {
    fun valider(deltaker: Deltaker)
}
