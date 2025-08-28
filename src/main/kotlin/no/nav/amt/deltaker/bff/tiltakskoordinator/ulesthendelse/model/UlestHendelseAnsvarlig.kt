package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface UlestHendelseAnsvarlig {
    data class NavVeileder(
        val id: UUID,
        val navn: String,
        val navIdent: String,
        val enhet: Enhet,
    ) : UlestHendelseAnsvarlig {
        data class Enhet(
            val id: UUID,
            val enhetsnummer: String,
        )
    }

    data class Deltaker(
        val id: UUID,
        val navn: String,
    ) : UlestHendelseAnsvarlig
}
