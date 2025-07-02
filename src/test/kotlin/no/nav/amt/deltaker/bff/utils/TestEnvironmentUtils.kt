package no.nav.amt.deltaker.bff.utils

import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.auth.PreAuthorizedApp
import java.nio.file.Paths
import java.util.UUID

fun configureEnvForAuthentication() {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL().toString()
    val preAuthorizedApp = PreAuthorizedApp("dev:tpts:tiltakspenger-tiltak", "tiltakspenger-tiltak")
    System.setProperty(Environment.AZURE_OPENID_CONFIG_JWKS_URI_KEY, uri)
    System.setProperty(Environment.AZURE_OPENID_CONFIG_ISSUER_KEY, "issuer")
    System.setProperty(Environment.AZURE_APP_CLIENT_ID_KEY, "deltaker-bff")
    System.setProperty(Environment.TOKEN_X_JWKS_URI_KEY, uri)
    System.setProperty(Environment.TOKEN_X_ISSUER_KEY, "issuer")
    System.setProperty(Environment.TOKEN_X_CLIENT_ID_KEY, "deltaker-bff")
    System.setProperty(Environment.AD_ROLLE_TILTAKSKOORDINATOR, UUID(0L, 0L).toString())
    System.setProperty(Environment.AZURE_APP_PRE_AUTHORIZED_APPS, objectMapper.writeValueAsString(listOf(preAuthorizedApp)))
}
