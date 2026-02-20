package no.nav.amt.deltaker.bff.deltaker.model

import java.time.LocalDateTime

data class VedtaksinformasjonModel(
    val fattet: LocalDateTime?,
    val fattetAvNav: Boolean,
    val opprettet: LocalDateTime,
    val opprettetAv: String,
    val opprettetAvEnhet: String,
    val sistEndret: LocalDateTime,
    val sistEndretAv: String?,
    val sistEndretAvEnhet: String?,
) {
    companion object {
        fun fromVedtaksinformasjonResponse(
            response: no.nav.amt.deltaker.bff.apiclients.deltaker.VedtaksinformasjonResponse?,
        ): VedtaksinformasjonModel? = response?.let {
            VedtaksinformasjonModel(
                fattet = it.fattet,
                fattetAvNav = it.fattetAvNav,
                opprettet = it.opprettet,
                opprettetAv = it.opprettetAv,
                opprettetAvEnhet = it.opprettetAvEnhet,
                sistEndret = it.sistEndret,
                sistEndretAv = it.sistEndretAv,
                sistEndretAvEnhet = it.sistEndretAvEnhet,
            )
        }
    }
}
