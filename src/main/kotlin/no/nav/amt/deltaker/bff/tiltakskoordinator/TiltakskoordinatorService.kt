package no.nav.amt.deltaker.bff.tiltakskoordinator

import no.nav.amt.deltaker.bff.apiclients.DtoMappers.toDeltakerOppdatering
import no.nav.amt.deltaker.bff.apiclients.distribusjon.AmtDistribusjonClient
import no.nav.amt.deltaker.bff.apiclients.tiltakskoordinator.TiltaksKoordinatorClient
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorTilgangRepository
import no.nav.amt.deltaker.bff.deltaker.DeltakerService
import no.nav.amt.deltaker.bff.deltaker.forslag.ForslagService
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.vurdering.VurderingService
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.amt.deltaker.bff.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.AvslagRequest
import no.nav.amt.deltaker.bff.tiltakskoordinator.extensions.toTiltakskoordinatorsDeltaker
import no.nav.amt.deltaker.bff.tiltakskoordinator.model.TiltakskoordinatorsDeltaker
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.internalapis.tiltakskoordinator.response.DeltakerOppdateringResponse
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import java.util.UUID

class TiltakskoordinatorService(
    private val tiltaksKoordinatorClient: TiltaksKoordinatorClient,
    private val deltakerService: DeltakerService,
    private val tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
    private val vurderingService: VurderingService,
    private val navEnhetService: NavEnhetService,
    private val navAnsattService: NavAnsattService,
    private val amtDistribusjonClient: AmtDistribusjonClient,
    private val forslagService: ForslagService,
) {
    suspend fun getMany(deltakerIder: List<UUID>) = deltakerService.getMany(deltakerIder).toTiltakskoordinatorsDeltaker()

    suspend fun getDeltaker(deltakerId: UUID): TiltakskoordinatorsDeltaker {
        val deltaker = deltakerService.getDeltaker(deltakerId).getOrThrow()
        val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
        val navVeileder = deltaker.navBruker.navVeilederId?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }
        val navEnhet = deltaker.navBruker.navEnhetId?.let { navEnhetService.hentEnhet(it) }
        val forslag = forslagService.getForDeltaker(deltaker.id)

        if (deltaker.navBruker.adresse == null) {
            val digitalBruker = amtDistribusjonClient.digitalBruker(deltaker.navBruker.personident)
            return deltaker.toTiltakskoordinatorsDeltaker(sisteVurdering, navEnhet, navVeileder, null, !digitalBruker, forslag)
        }

        return deltaker.toTiltakskoordinatorsDeltaker(sisteVurdering, navEnhet, navVeileder, null, false, forslag)
    }

    suspend fun endreDeltakere(
        deltakerIder: List<UUID>,
        endring: EndringFraTiltakskoordinator.Endring,
        endretAv: String,
    ): List<TiltakskoordinatorsDeltaker> {
        val oppdaterteDeltakereResponses = when (endring) {
            EndringFraTiltakskoordinator.SettPaaVenteliste -> tiltaksKoordinatorClient.settPaaVenteliste(deltakerIder, endretAv)
            EndringFraTiltakskoordinator.DelMedArrangor -> tiltaksKoordinatorClient.delMedArrangor(deltakerIder, endretAv)
            EndringFraTiltakskoordinator.TildelPlass -> tiltaksKoordinatorClient.tildelPlass(deltakerIder, endretAv)
            is EndringFraTiltakskoordinator.Avslag -> throw NotImplementedError("Batch håndtering for avslag er ikke støttet")
        }
        val deltakerOppdateringer = oppdaterteDeltakereResponses.map { it.toDeltakerOppdatering() }
        deltakerService.oppdaterDeltakere(deltakerOppdateringer)

        return oppdaterteDeltakereResponses.toTiltakskoordinatorsDeltakere()
    }

    suspend fun giAvslag(request: AvslagRequest, endretAv: String): TiltakskoordinatorsDeltaker {
        val deltakeroppdatering = tiltaksKoordinatorClient.giAvslag(request, endretAv)

        deltakerService.oppdaterDeltaker(deltakeroppdatering)

        return deltakerService.getDeltaker(deltakeroppdatering.id).getOrThrow().toTiltakskoordinatorsDeltaker()
    }

    fun hentKoordinatorer(deltakerlisteId: UUID) = tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)

    suspend fun hentDeltakereForDeltakerliste(deltakerlisteId: UUID): List<TiltakskoordinatorsDeltaker> {
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

    private suspend fun Deltaker.toTiltakskoordinatorsDeltaker() = listOf(this).toTiltakskoordinatorsDeltaker().first()

    private suspend fun List<Deltaker>.toTiltakskoordinatorsDeltaker(): List<TiltakskoordinatorsDeltaker> {
        val navEnheter = navEnhetService.hentEnheter(this.mapNotNull { it.navBruker.navEnhetId })
        val navVeiledere = navAnsattService.hentAnsatte(this.mapNotNull { it.navBruker.navVeilederId })
        val forslag = forslagService.getForDeltakere(this.map { it.id })

        return this
            .map {
                val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(it.id)

                var ikkeDigitalOgManglerAdresse = false
                if (it.navBruker.adresse == null) {
                    ikkeDigitalOgManglerAdresse = !amtDistribusjonClient.digitalBruker(it.navBruker.personident)
                }

                it.toTiltakskoordinatorsDeltaker(
                    sisteVurdering,
                    navEnheter[it.navBruker.navEnhetId],
                    navVeiledere[it.navBruker.navVeilederId],
                    null,
                    ikkeDigitalOgManglerAdresse,
                    forslag.filter { forslag -> forslag.deltakerId == it.id },
                )
            }.filterNot { it.skalSkjules() }
    }

    private suspend fun List<DeltakerOppdateringResponse>.toTiltakskoordinatorsDeltakere(): List<TiltakskoordinatorsDeltaker> {
        val deltakere = deltakerService.getMany(this.map { it.id })
        val navEnheter = navEnhetService.hentEnheter(deltakere.mapNotNull { it.navBruker.navEnhetId })
        val navVeiledere = navAnsattService.hentAnsatte(deltakere.mapNotNull { it.navBruker.navVeilederId })
        val forslag = forslagService.getForDeltakere(this.map { it.id })

        return deltakere
            .map { deltaker ->
                val sisteVurdering = vurderingService.getSisteVurderingForDeltaker(deltaker.id)
                var ikkeDigitalOgManglerAdresse = false
                if (deltaker.navBruker.adresse == null) {
                    ikkeDigitalOgManglerAdresse = !amtDistribusjonClient.digitalBruker(deltaker.navBruker.personident)
                }
                deltaker.toTiltakskoordinatorsDeltaker(
                    sisteVurdering,
                    navEnheter[deltaker.navBruker.navEnhetId],
                    navVeiledere[deltaker.navBruker.navVeilederId],
                    first { it.id == deltaker.id }.feilkode,
                    ikkeDigitalOgManglerAdresse,
                    forslag.filter { forslag -> forslag.deltakerId == deltaker.id },
                )
            }.filterNot { it.skalSkjules() }
    }
}
