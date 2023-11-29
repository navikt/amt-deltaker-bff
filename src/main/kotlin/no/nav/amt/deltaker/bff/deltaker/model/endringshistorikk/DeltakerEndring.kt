package no.nav.amt.deltaker.bff.deltaker.model.endringshistorikk

import no.nav.amt.deltaker.bff.deltakerliste.Mal
import java.time.LocalDate

sealed class DeltakerEndring {
    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : DeltakerEndring()

    data class EndreMal(
        val mal: List<Mal>,
    ) : DeltakerEndring()

    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Float?,
        val dagerPerUke: Float?,
    ) : DeltakerEndring()

    data class EndreStartdato(
        val startdato: LocalDate?,
    ) : DeltakerEndring()

    data class EndreSluttdato(
        val sluttdato: LocalDate?,
    ) : DeltakerEndring()
}
