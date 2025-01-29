package no.nav.amt.deltaker.bff.auth

import no.nav.amt.deltaker.bff.auth.model.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.navansatt.NavAnsattService
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.EksternBrukerTilgangTilEksternBrukerPolicyInput
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

    suspend fun leggTilTiltakskoordinatorTilgang(navIdent: String, deltakerlisteId: UUID): Result<TiltakskoordinatorDeltakerlisteTilgang> {
        val koordinator = navAnsattService.hentEllerOpprettNavAnsatt(navIdent)
        val eksisterendeTilgang = tiltakskoordinatorTilgangRepository.hentAktivTilgang(koordinator.id, deltakerlisteId)

        if (eksisterendeTilgang.isSuccess) {
            log.error(
                "Kan ikke legge til tilgang til deltakerliste $deltakerlisteId" +
                    " fordi nav-ansatt ${koordinator.id} har allerede tilgang fra før.",
            )
            return Result.failure(IllegalStateException("Nav-ansatt ${koordinator.id} har allerede tilgang til $deltakerlisteId"))
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
}
