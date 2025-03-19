package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
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
import no.nav.amt.lib.models.tiltakskoordinator.response.EndringFraTiltakskoordinatorResponse
import java.util.UUID

class TiltakskoordinatorService(
    private val amtDeltakerClient: AmtDeltakerClient,
    private val deltakerService: DeltakerService,
    private val tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
    private val vurderingService: VurderingService,
    private val navEnhetService: NavEnhetService,
    private val navAnsattService: NavAnsattService,
) {
    suspend fun get(deltakerId: UUID): TiltakskoordinatorsDeltaker {
        val deltaker = deltakerService.get(deltakerId).getOrThrow()
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        val navVeileder = deltaker.navBruker.navVeilederId?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }
        val navEnhet = deltaker.navBruker.navEnhetId?.let { navEnhetService.hentEnhet(it) }

        return TiltakskoordinatorsDeltaker(
            id = deltaker.id,
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = deltaker.status,
            startdato = deltaker.startdato,
            sluttdato = deltaker.sluttdato,
            navEnhet = navEnhet?.navn,
            navVeileder = NavVeileder(
                navn = navVeileder?.navn,
                telefonnummer = null,
                epost = null,
            ),
            beskyttelsesmarkering = deltaker.navBruker.getBeskyttelsesmarkeringer(),
            vurdering = sisteVurdering,
            innsatsgruppe = deltaker.navBruker.innsatsgruppe,
            erManueltDeltMedArrangor = deltaker.erManueltDeltMedArrangor,
        )
    }

    suspend fun endreDeltakere(
        deltakerIder: List<UUID>,
        endring: EndringFraTiltakskoordinator.Endring,
        endretAv: String,
    ): List<TiltakskoordinatorsDeltaker> {
        val oppdateringer = when (endring) {
            EndringFraTiltakskoordinator.DelMedArrangor -> amtDeltakerClient.delMedArrangor(deltakerIder, endretAv)
        }

        val deltakere = deltakerService.getMany(deltakerIder).associateBy { it.id }
        val oppdaterteDeltakere = oppdateringer.mapNotNull { oppdatering ->
            deltakere[oppdatering.id]?.oppdater(oppdatering)
        }

        deltakerService.oppdaterDeltakere(oppdaterteDeltakere)

        return oppdaterteDeltakere.toTiltakskoordinatorsDeltaker()
    }

    fun hentKoordinatorer(deltakerlisteId: UUID) = tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)

    fun hentDeltakere(deltakerlisteId: UUID): List<TiltakskoordinatorsDeltaker> {
        val deltakere = deltakerService.getForDeltakerliste(deltakerlisteId)
        val navEnheter = navEnhetService.hentEnheter(deltakere.mapNotNull { it.navBruker.navEnhetId })
        val navVeiledere = navAnsattService.hentAnsatte(deltakere.mapNotNull { it.navBruker.navVeilederId })
        return deltakere
            .map {
                val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(it.id)
                it.toTiltakskoordinatorsDeltaker(
                    sisteVurdering,
                    navEnheter[it.navBruker.navEnhetId],
                    navVeiledere[it.navBruker.navVeilederId],
                )
            }.filterNot { deltaker -> deltaker.skalSkjules() }
    }

    fun Deltaker.oppdater(endring: EndringFraTiltakskoordinatorResponse) = this.copy(
        erManueltDeltMedArrangor = endring.erDeltManueltMedArrangor,
        sistEndret = endring.sistEndret,
    )

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
        }
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
        telefonnummer = null,
        epost = null,
    ),
    beskyttelsesmarkering = navBruker.getBeskyttelsesmarkeringer(),
    vurdering = sisteVurdering,
    innsatsgruppe = navBruker.innsatsgruppe,
    deltakerliste = deltakerliste,
    erManueltDeltMedArrangor = erManueltDeltMedArrangor,
)
