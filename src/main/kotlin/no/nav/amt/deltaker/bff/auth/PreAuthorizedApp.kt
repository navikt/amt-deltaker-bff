package no.nav.amt.deltaker.bff.auth

import kotlin.collections.last
import kotlin.text.split

data class PreAuthorizedApp(
    val name: String,
    val clientId: String,
) {
    val appName = name.split(":").last()
    val team = name.split(":")[1]
}
