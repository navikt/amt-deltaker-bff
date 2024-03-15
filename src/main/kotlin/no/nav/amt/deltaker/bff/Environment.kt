package no.nav.amt.deltaker.bff

data class Environment(
    val dbUsername: String = getEnvVar(DB_USERNAME_KEY),
    val dbPassword: String = getEnvVar(DB_PASSWORD_KEY),
    val dbDatabase: String = getEnvVar(DB_DATABASE_KEY),
    val dbHost: String = getEnvVar(DB_HOST_KEY),
    val dbPort: String = getEnvVar(DB_PORT_KEY),
    val azureAdTokenUrl: String = getEnvVar(AZURE_AD_TOKEN_URL_KEY),
    val azureClientId: String = getEnvVar(AZURE_APP_CLIENT_ID_KEY),
    val azureClientSecret: String = getEnvVar(AZURE_APP_CLIENT_SECRET_KEY),
    val azureJwkKeysUrl: String = getEnvVar(AZURE_OPENID_CONFIG_JWKS_URI_KEY),
    val azureJwtIssuer: String = getEnvVar(AZURE_OPENID_CONFIG_ISSUER_KEY),
    val amtArrangorUrl: String = getEnvVar(AMT_ARRANGOR_URL_KEY),
    val amtArrangorScope: String = getEnvVar(AMT_ARRANGOR_SCOPE_KEY),
    val poaoTilgangUrl: String = getEnvVar(POAO_TILGANG_URL_KEY),
    val poaoTilgangScope: String = getEnvVar(POAO_TILGANG_SCOPE_KEY),
    val amtPersonServiceUrl: String = getEnvVar(AMT_PERSONSERVICE_URL_KEY),
    val amtPersonServiceScope: String = getEnvVar(AMT_PERSONSERVICE_SCOPE_KEY),
    val amtDeltakerUrl: String = getEnvVar(AMT_DELTAKER_URL_KEY),
    val amtDeltakerScope: String = getEnvVar(AMT_DELTAKER_SCOPE_KEY),
    val tokenXJwksUrl: String = getEnvVar(TOKEN_X_JWKS_URI_KEY),
    val tokenXJwtIssuer: String = getEnvVar(TOKEN_X_ISSUER_KEY),
    val tokenXClientId: String = getEnvVar(TOKEN_X_CLIENT_ID_KEY),
) {
    companion object {
        const val DB_USERNAME_KEY = "DB_USERNAME"
        const val DB_PASSWORD_KEY = "DB_PASSWORD"
        const val DB_DATABASE_KEY = "DB_DATABASE"
        const val DB_HOST_KEY = "DB_HOST"
        const val DB_PORT_KEY = "DB_PORT"

        const val KAFKA_CONSUMER_GROUP_ID = "amt-deltaker-bff-consumer"
        const val DELTAKERLISTE_TOPIC = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1"
        const val AMT_ENDRINGSMELDING_TOPIC = "amt.endringsmelding-v1"
        const val TILTAKSTYPE_TOPIC = "team-mulighetsrommet.siste-tiltakstyper-v1"

        const val AMT_ARRANGOR_TOPIC = "amt.arrangor-v1"
        const val AMT_ARRANGOR_URL_KEY = "AMT_ARRANGOR_URL"
        const val AMT_ARRANGOR_SCOPE_KEY = "AMT_ARRANGOR_SCOPE"

        const val AMT_DELTAKERV2_TOPIC = "amt.deltaker-v2"
        const val AMT_NAV_BRUKER_TOPIC = "amt.nav-bruker-personalia-v1"
        const val AMT_NAV_ANSATT_TOPIC = "amt.nav-ansatt-personalia-v1"
        const val AMT_PERSONSERVICE_URL_KEY = "AMT_PERSONSERVICE_URL"
        const val AMT_PERSONSERVICE_SCOPE_KEY = "AMT_PERSONSERVICE_SCOPE"

        const val AMT_DELTAKER_URL_KEY = "AMT_DELTAKER_URL"
        const val AMT_DELTAKER_SCOPE_KEY = "AMT_DELTAKER_SCOPE"

        const val AZURE_AD_TOKEN_URL_KEY = "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"
        const val AZURE_APP_CLIENT_SECRET_KEY = "AZURE_APP_CLIENT_SECRET"
        const val AZURE_APP_CLIENT_ID_KEY = "AZURE_APP_CLIENT_ID"
        const val AZURE_OPENID_CONFIG_JWKS_URI_KEY = "AZURE_OPENID_CONFIG_JWKS_URI"
        const val AZURE_OPENID_CONFIG_ISSUER_KEY = "AZURE_OPENID_CONFIG_ISSUER"

        const val TOKEN_X_CLIENT_ID_KEY = "TOKEN_X_CLIENT_ID"
        const val TOKEN_X_ISSUER_KEY = "TOKEN_X_ISSUER"
        const val TOKEN_X_JWKS_URI_KEY = "TOKEN_X_JWKS_URI"

        const val POAO_TILGANG_URL_KEY = "POAO_TILGANG_URL"
        const val POAO_TILGANG_SCOPE_KEY = "POAO_TILGANG_SCOPE"

        const val HTTP_CLIENT_TIMEOUT_MS = 10_000

        fun isDev(): Boolean {
            val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke dev"
            return cluster == "dev-gcp"
        }

        fun isProd(): Boolean {
            val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke prod"
            return cluster == "prod-gcp"
        }

        fun isLocal(): Boolean {
            return !isDev() && !isProd()
        }
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) = System.getenv(varName)
    ?: System.getProperty(varName)
    ?: defaultValue
    ?: if (Environment.isLocal()) "" else error("Missing required variable $varName")
