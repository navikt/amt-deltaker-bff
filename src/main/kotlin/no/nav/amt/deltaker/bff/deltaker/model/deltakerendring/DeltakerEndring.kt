package no.nav.amt.deltaker.bff.deltaker.model.deltakerendring

import no.nav.amt.deltaker.bff.deltakerliste.Mal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerEndring(
    val id: UUID,
    val deltakerId: UUID,
    val endringstype: Endringstype,
    val endring: Endring,
    val endretAv: String,
    val endretAvEnhet: String?,
    val endret: LocalDateTime,
) {
    enum class Endringstype {
        STARTDATO, SLUTTDATO, DELTAKELSESMENGDE, BAKGRUNNSINFORMASJON, MAL
    }

    sealed class Endring {
        data class EndreBakgrunnsinformasjon(
            val bakgrunnsinformasjon: String?,
        ) : Endring()

        data class EndreMal(
            val mal: List<Mal>,
        ) : Endring()

        data class EndreDeltakelsesmengde(
            val deltakelsesprosent: Float?,
            val dagerPerUke: Float?,
        ) : Endring()

        data class EndreStartdato(
            val startdato: LocalDate?,
        ) : Endring()

        data class EndreSluttdato(
            val sluttdato: LocalDate?,
        ) : Endring()
    }
}
