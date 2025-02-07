package no.nav.amt.deltaker.bff.auth

import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerTilgang
import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.Adressebeskyttelse
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.navansatt.NavAnsatt
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.EksternBrukerTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.NavAnsattBehandleFortroligBrukerePolicyInput
import no.nav.poao_tilgang.client.NavAnsattBehandleSkjermedePersonerPolicyInput
import no.nav.poao_tilgang.client.NavAnsattBehandleStrengtFortroligBrukerePolicyInput
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.TilgangType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class TilgangskontrollService(
    private val poaoTilgangCachedClient: PoaoTilgangCachedClient,
    private val navAnsattService: NavAnsattService,
    private val tiltakskoordinatorTilgangRepository: TiltakskoordinatorTilgangRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun verifiserSkrivetilgang(navAnsattAzureId: UUID, norskIdent: String) {
        val tilgang = poaoTilgangCachedClient
            .evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    navAnsattAzureId,
                    TilgangType.SKRIVE,
                    norskIdent,
                ),
            ).getOrDefault(Decision.Deny("Ansatt har ikke skrivetilgang til bruker", ""))

        if (tilgang.isDeny) {
            throw AuthorizationException("Ansatt har ikke skrivetilgang til bruker")
        }
    }

    fun verifiserLesetilgang(navAnsattAzureId: UUID, norskIdent: String) {
        val tilgang = poaoTilgangCachedClient
            .evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    navAnsattAzureId,
                    TilgangType.LESE,
                    norskIdent,
                ),
            ).getOrDefault(Decision.Deny("Ansatt har ikke lesetilgang til bruker", ""))

        if (tilgang.isDeny) {
            throw AuthorizationException("Ansatt har ikke lesetilgang til bruker")
        }
    }

    fun verifiserInnbyggersTilgangTilDeltaker(rekvirentPersonident: String, ressursPersonident: String) {
        val tilgang = poaoTilgangCachedClient
            .evaluatePolicy(
                EksternBrukerTilgangTilEksternBrukerPolicyInput(rekvirentPersonident, ressursPersonident),
            ).getOrDefault(Decision.Deny("Innbygger har ikke tilgang til deltaker", ""))

        if (tilgang.isDeny) {
            throw AuthorizationException("Innbygger har ikke tilgang til deltaker")
        }
    }

    fun vurderKoordinatorTilgangTilDeltaker(navAnsattAzureId: UUID, deltaker: Deltaker): TiltakskoordinatorDeltakerTilgang {
        val tilgangTilAdressebeskyttelse = vurderAdressebeskyttelseTilgang(deltaker.navBruker.adressebeskyttelse, navAnsattAzureId)
        val tilgangTilSkjerming = vurderSkjermingTilgang(deltaker.navBruker, navAnsattAzureId)

        return TiltakskoordinatorDeltakerTilgang(deltaker, tilgangTilAdressebeskyttelse.isPermit && tilgangTilSkjerming.isPermit)
    }

    private fun vurderSkjermingTilgang(navBruker: NavBruker, navAnsattAzureId: UUID): Decision = if (navBruker.erSkjermet) {
        poaoTilgangCachedClient.evaluatePolicy(NavAnsattBehandleSkjermedePersonerPolicyInput(navAnsattAzureId)).getOrThrow()
    } else {
        Decision.Permit
    }

    private fun vurderAdressebeskyttelseTilgang(adressebeskyttelse: Adressebeskyttelse?, navAnsattAzureId: UUID): Decision =
        when (adressebeskyttelse) {
            Adressebeskyttelse.FORTROLIG ->
                poaoTilgangCachedClient.evaluatePolicy(NavAnsattBehandleFortroligBrukerePolicyInput(navAnsattAzureId)).getOrThrow()

            Adressebeskyttelse.STRENGT_FORTROLIG, Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND ->
                poaoTilgangCachedClient.evaluatePolicy(NavAnsattBehandleStrengtFortroligBrukerePolicyInput(navAnsattAzureId)).getOrThrow()

            else -> Decision.Permit
        }

    suspend fun leggTilTiltakskoordinatorTilgang(navIdent: String, deltakerlisteId: UUID): Result<TiltakskoordinatorDeltakerlisteTilgang> {
        val koordinator = navAnsattService.hentEllerOpprettNavAnsatt(navIdent)
        val aktivTilgang = tiltakskoordinatorTilgangRepository.hentAktivTilgang(koordinator.id, deltakerlisteId)

        if (aktivTilgang.isSuccess) {
            log.error(
                "Kan ikke legge til tilgang til deltakerliste $deltakerlisteId " +
                    "fordi nav-ansatt ${koordinator.id} har allerede tilgang fra før.",
            )
            return Result.failure(IllegalArgumentException("Nav-ansatt ${koordinator.id} har allerede tilgang til $deltakerlisteId"))
        }

        val tilgang = TiltakskoordinatorDeltakerlisteTilgang(
            id = UUID.randomUUID(),
            navAnsattId = koordinator.id,
            deltakerlisteId = deltakerlisteId,
            gyldigFra = LocalDateTime.now(),
            gyldigTil = null,
        )

        return tiltakskoordinatorTilgangRepository.upsert(tilgang)
    }

    suspend fun verifiserTiltakskoordinatorTilgang(navIdent: String, deltakerlisteId: UUID) {
        val koordinator = navAnsattService.hentEllerOpprettNavAnsatt(navIdent)
        val aktivTilgang = tiltakskoordinatorTilgangRepository.hentAktivTilgang(koordinator.id, deltakerlisteId)

        if (aktivTilgang.isFailure) {
            throw AuthorizationException("Ansatt ${koordinator.id} har ikke tilgang til deltakerliste $deltakerlisteId")
        }
    }

    fun hentKoordinatorer(deltakerlisteId: UUID): List<NavAnsatt> {
        return tiltakskoordinatorTilgangRepository.hentKoordinatorer(deltakerlisteId)
    }
}
