package no.nav.amt.deltaker.bff.application.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.auth.AuthenticationException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

fun Application.configureAuthentication(environment: Environment) {
    val jwkProvider = JwkProviderBuilder(URI(environment.jwkKeysUrl).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt("VEILEDER") {
            verifier(jwkProvider, environment.jwtIssuer) {
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
    }
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getNavAnsattAzureId(): UUID {
    return call.principal<JWTPrincipal>()
        ?.get("oid")
        ?.let { UUID.fromString(it) }
        ?: throw AuthenticationException("NavAnsattAzureId mangler i JWTPrincipal")
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getNavIdent(): String {
    return call.principal<JWTPrincipal>()
        ?.get("NAVident")
        ?: throw AuthenticationException("NAVident mangler i JWTPrincipal")
}
