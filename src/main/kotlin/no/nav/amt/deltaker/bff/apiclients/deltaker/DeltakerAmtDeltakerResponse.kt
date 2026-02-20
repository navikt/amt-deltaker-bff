package no.nav.amt.deltaker.bff.apiclients.deltaker

import no.nav.amt.deltaker.bff.deltaker.model.DeltakerModel
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Kilde
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerAmtDeltakerResponse(
    val id: UUID,
    val status: DeltakerStatus,
    val navBruker: NavBrukerResponse,
    val gjennomforing: GjennomforingResponse,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val vedtaksinformasjon: VedtaksinformasjonResponse?,
    val erManueltDeltMedArrangor: Boolean,
    val kilde: Kilde,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
    /*
    Må vi ha med historikk?
        Ja, virker sånn fordi mye informasjon utledes fra historikken i domeneobjektet
        Bør vurdere å utlede dataene i amt-deltaker og sende strukturert?
     */
    val historikk: List<DeltakerHistorikk>,
    val erLaastForEndringer: Boolean, // TODO: Må fikses i amt-deltaker
    val endringsforslagFraArrangor: List<Forslag>,
) {
    fun toDeltakerModel() = DeltakerModel(
        id = id,
        navBruker = navBruker.toNavBrukerModel(),
        gjennomforing = this.gjennomforing.toGjennomforingModel(),
        startdato = startdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = deltakelsesprosent,
        bakgrunnsinformasjon = bakgrunnsinformasjon,
        deltakelsesinnhold = deltakelsesinnhold,
        status = status,
        kanEndres = !erLaastForEndringer,
        sistEndret = sistEndret,
        erManueltDeltMedArrangor = erManueltDeltMedArrangor,
        historikk = historikk,
        vedtaksinformasjon = vedtaksinformasjon?.toVedtaksinformasjonModel(),
        erLaastForEndringer = erLaastForEndringer,
        endringsforslagFraArrangor = endringsforslagFraArrangor,
    )
}
