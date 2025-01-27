package no.nav.amt.deltaker.bff.application.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.auth.AuthenticationException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

const val ID_PORTEN_LEVEL4 = "Level4"
const val ID_PORTEN_LOA_HIGH = "idporten-loa-high"

enum class AuthLevel {
    INNBYGGER,
    VEILEDER,
    TILTAKSKOORDINATOR,
}

fun Application.configureAuthentication(environment: Environment) {
    val azureJwkProvider = JwkProviderBuilder(URI(environment.azureJwkKeysUrl).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    val tokenXJwkProvider = JwkProviderBuilder(URI(environment.tokenXJwksUrl).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt(AuthLevel.INNBYGGER.name) {
            verifier(tokenXJwkProvider, environment.tokenXJwtIssuer) {
                withAudience(environment.tokenXClientId)
            }
            validate {
                if (it["pid"] == null) {
                    application.log.warn("Ikke tilgang. Token mangler claim 'pid'.")
                    return@validate null
                }
                if (it["acr"] != ID_PORTEN_LEVEL4 && it["acr"] != ID_PORTEN_LOA_HIGH) {
                    application.log.warn("Ikke tilgang. Token mangler gyldig 'acr' claim.")
                    return@validate null
                }
                JWTPrincipal(it.payload)
            }
        }

        jwt(AuthLevel.VEILEDER.name) {
            verifier(azureJwkProvider, environment.azureJwtIssuer) {
                withAudience(environment.azureClientId)
            }

            validate { credentials ->
                credentials["NAVident"] ?: run {
                    application.log.warn("Ikke tilgang. Mangler claim 'NAVident'.")
                    return@validate null
                }
                JWTPrincipal(credentials.payload)
            }
        }
        jwt(AuthLevel.TILTAKSKOORDINATOR.name) {
            verifier(azureJwkProvider, environment.azureJwtIssuer) {
                withAudience(environment.azureClientId)
            }

            validate { credentials ->
                credentials["NAVident"] ?: run {
                    application.log.warn("Ikke tilgang. Mangler claim 'NAVident'.")
                    return@validate null
                }
                // Sjekk ad-gruppe...

                JWTPrincipal(credentials.payload)
            }
        }
    }
}

fun ApplicationCall.getNavAnsattAzureId(): UUID = this
    .principal<JWTPrincipal>()
    ?.get("oid")
    ?.let { UUID.fromString(it) }
    ?: throw AuthenticationException("NavAnsattAzureId mangler i JWTPrincipal")

fun ApplicationCall.getNavIdent(): String = this
    .principal<JWTPrincipal>()
    ?.get("NAVident")
    ?: throw AuthenticationException("NAVident mangler i JWTPrincipal")

fun ApplicationCall.getPersonident(): String = this
    .principal<JWTPrincipal>()
    ?.get("pid")
    ?: throw AuthenticationException("Pid mangler i JWTPrincipal")
