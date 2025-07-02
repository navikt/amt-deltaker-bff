package no.nav.amt.deltaker.bff.tiltakskoordinator.model

import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.DeltakerOppdateringFeilkode
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import java.time.LocalDate
import java.util.UUID

data class TiltakskoordinatorsDeltaker(
    val id: UUID,
    val navBruker: NavBruker,
    val status: DeltakerStatus,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val navEnhet: String?,
    val navVeileder: NavVeileder,
    val beskyttelsesmarkering: List<Beskyttelsesmarkering>,
    val vurdering: Vurdering?,
    val innsatsgruppe: Innsatsgruppe?,
    val deltakerliste: Deltakerliste,
    val erManueltDeltMedArrangor: Boolean,
    val kanEndres: Boolean,
    val feilkode: DeltakerOppdateringFeilkode? = null,
    val ikkeDigitalOgManglerAdresse: Boolean,
    val forslag: List<Forslag>,
)
