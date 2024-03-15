package no.nav.amt.deltaker.bff.utils

import no.nav.amt.deltaker.bff.Environment
import java.nio.file.Paths

fun configureEnvForAuthentication() {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL().toString()
    System.setProperty(Environment.AZURE_OPENID_CONFIG_JWKS_URI_KEY, uri)
    System.setProperty(Environment.AZURE_OPENID_CONFIG_ISSUER_KEY, "issuer")
    System.setProperty(Environment.AZURE_APP_CLIENT_ID_KEY, "deltaker-bff")
    System.setProperty(Environment.TOKEN_X_JWKS_URI_KEY, uri)
    System.setProperty(Environment.TOKEN_X_ISSUER_KEY, "issuer")
    System.setProperty(Environment.TOKEN_X_CLIENT_ID_KEY, "deltaker-bff")
}
