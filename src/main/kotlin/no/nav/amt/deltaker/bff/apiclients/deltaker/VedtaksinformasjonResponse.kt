package no.nav.amt.deltaker.bff.apiclients.deltaker

import java.time.LocalDateTime

data class VedtaksinformasjonResponse(
    val fattet: LocalDateTime?,
    val fattetAvNav: Boolean,
    val opprettet: LocalDateTime,
    val opprettetAv: String, // Det er nytt at dette er en string
    val opprettetAvEnhet: String,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String?, // Det er nytt at dette er en string
    val sistEndretAvEnhet: String?, // Det er nytt at dette er en string
)
