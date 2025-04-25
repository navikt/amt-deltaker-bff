package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhet
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.NavVeileder
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import java.util.UUID

class TiltakskoordinatorService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
    private val tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
    private val vurderingService: VurderingService,
    private val navEnhetService: NavEnhetService,
    private val navAnsattService: NavAnsattService,
) {
    fun getMany(deltakerIder: List<UUID>) = deltakerService.getMany(deltakerIder).toTiltakskoordinatorsDeltaker()

    suspend fun get(deltakerId: UUID): TiltakskoordinatorsDeltaker {
        val deltaker = deltakerService.get(deltakerId).getOrThrow()
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        val navVeileder = deltaker.navBruker.navVeilederId?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }
        val navEnhet = deltaker.navBruker.navEnhetId?.let { navEnhetService.hentEnhet(it) }

        return deltaker.toTiltakskoordinatorsDeltaker(sisteVurdering, navEnhet, navVeileder)
    }

    suspend fun endreDeltakere(
        deltakerIder: List<UUID>,
        endring: EndringFraTiltakskoordinator.Endring,
        endretAv: String,
    ): List<TiltakskoordinatorsDeltaker> {
        val oppdaterteDeltakere = when (endring) {
            EndringFraTiltakskoordinator.SettPaaVenteliste -> amtDeltakerClient.settPaaVenteliste(deltakerIder, endretAv)
            EndringFraTiltakskoordinator.DelMedArrangor -> amtDeltakerClient.delMedArrangor(deltakerIder, endretAv)
        }

        deltakerService.oppdaterDeltakere(oppdaterteDeltakere)

        return oppdaterteDeltakere.toTiltakskoordinatorsDeltakere()
    }

    fun hentKoordinatorer(deltakerlisteId: UUID) = tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)

    fun hentDeltakereForDeltakerliste(deltakerlisteId: UUID): List<TiltakskoordinatorsDeltaker> {
        val deltakere = deltakerService.getForDeltakerliste(deltakerlisteId)
        return deltakere
            .toTiltakskoordinatorsDeltaker()
    }

    fun TiltakskoordinatorsDeltaker.skalSkjules() = status.type in listOf(
        DeltakerStatus.Type.KLADD,
        DeltakerStatus.Type.UTKAST_TIL_PAMELDING,
        DeltakerStatus.Type.AVBRUTT_UTKAST,
        DeltakerStatus.Type.FEILREGISTRERT,
        DeltakerStatus.Type.PABEGYNT_REGISTRERING,
    )

    private fun List<Deltaker>.toTiltakskoordinatorsDeltaker(): List<TiltakskoordinatorsDeltaker> {
        val navEnheter = navEnhetService.hentEnheter(this.mapNotNull { it.navBruker.navEnhetId })
        val navVeiledere = navAnsattService.hentAnsatte(this.mapNotNull { it.navBruker.navVeilederId })
        return this.map {
            val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(it.id)
            it.toTiltakskoordinatorsDeltaker(
                sisteVurdering,
                navEnheter[it.navBruker.navEnhetId],
                navVeiledere[it.navBruker.navVeilederId],
            )
        }.filterNot { it.skalSkjules() }
    }

    private fun List<Deltakeroppdatering>.toTiltakskoordinatorsDeltakere(): List<TiltakskoordinatorsDeltaker> {
        val deltakere = deltakerService.getMany(this.map { it.id })
        return deltakere.toTiltakskoordinatorsDeltaker()
    }
}

fun Deltaker.toTiltakskoordinatorsDeltaker(
    sisteVurdering: Vurdering?,
    navEnhet: NavEnhet?,
    navVeileder: NavAnsatt?,
) = TiltakskoordinatorsDeltaker(
    id = id,
    navBruker = navBruker,
    status = status,
    startdato = startdato,
    sluttdato = sluttdato,
    navEnhet = navEnhet?.navn,
    navVeileder = NavVeileder(
        navn = navVeileder?.navn,
        telefonnummer = navVeileder?.telefon,
        epost = navVeileder?.epost,
    ),
    beskyttelsesmarkering = navBruker.getBeskyttelsesmarkeringer(),
    vurdering = sisteVurdering,
    innsatsgruppe = navBruker.innsatsgruppe,
    deltakerliste = deltakerliste,
    erManueltDeltMedArrangor = erManueltDeltMedArrangor,
)
