package no.nav.amt.deltaker.bff.apiclients.deltaker

import no.nav.amt.deltaker.bff.deltaker.model.VedtaksinformasjonModel
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
) {
    fun toVedtaksinformasjonModel() = VedtaksinformasjonModel(
        fattet = fattet,
        fattetAvNav = fattetAvNav,
        opprettet = opprettet,
        opprettetAv = opprettetAv,
        opprettetAvEnhet = opprettetAvEnhet,
        sistEndret = sistEndret,
        sistEndretAv = sistEndretAv,
        sistEndretAvEnhet = sistEndretAvEnhet,
    )
}
