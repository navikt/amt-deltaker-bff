package no.nav.amt.deltaker.bff.deltaker.model.deltakerendring

import no.nav.amt.deltaker.bff.deltakerliste.Mal
import java.time.LocalDate

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
