package no.nav.amt.deltaker.bff.deltaker.amtdeltaker

import no.nav.amt.deltaker.bff.arrangor.Arrangor
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.DeltakerStatus
import no.nav.amt.deltaker.bff.deltaker.model.Innhold
import no.nav.amt.deltaker.bff.deltaker.navbruker.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AmtDeltakerDto(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerliste: DeltakerlisteDto,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
    val status: DeltakerStatus,
    val sistEndretAv: NavAnsatt,
    val sistEndretAvEnhet: NavEnhet,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
) {

    data class DeltakerlisteDto(
        val id: UUID,
        val tiltakstype: Tiltakstype,
        val navn: String,
        val status: Deltakerliste.Status,
        val startDato: LocalDate,
        val sluttDato: LocalDate?,
        val oppstart: Deltakerliste.Oppstartstype?,
        val arrangor: Arrangor,
    ) {
        fun toModel() = Deltakerliste(
            id = id,
            tiltak = tiltakstype,
            navn = navn,
            status = status,
            startDato = startDato,
            sluttDato = sluttDato,
            oppstart = oppstart,
            arrangor = Deltakerliste.Arrangor(arrangor, null),
        )
    }

    fun toModel() = Deltaker(
        id = id,
        navBruker = navBruker,
        deltakerliste = deltakerliste.toModel(),
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        innhold = innhold,
        status = status,
        vedtaksinformasjon = null,
        sistEndretAv = sistEndretAv.navIdent,
        sistEndretAvEnhet = sistEndretAvEnhet.enhetsnummer,
        sistEndret = sistEndret,
        opprettet = opprettet,
    )
}
