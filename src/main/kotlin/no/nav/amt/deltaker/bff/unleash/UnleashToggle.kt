package no.nav.amt.deltaker.bff.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode

class UnleashToggle(
    private val unleashClient: Unleash,
) {
    fun erKometMasterForTiltakstype(tiltakskode: String): Boolean = tiltakstyperKometErMasterFor.any { it.name == tiltakskode } ||
        (unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) && tiltakstyperKometKanskjeErMasterFor.any { it.name == tiltakskode })

    fun erKometMasterForTiltakstype(tiltakskode: Tiltakskode): Boolean = erKometMasterForTiltakstype(tiltakskode.name)

    fun skalLeseArenaDataForTiltakstype(tiltakskode: String): Boolean =
        unleashClient.isEnabled(LES_ARENA_DELTAKERE) && tiltakstyperKometKanLese.any { it.name == tiltakskode }

    fun skalLeseArenaDataForTiltakstype(tiltakskode: Tiltakskode): Boolean = skalLeseArenaDataForTiltakstype(tiltakskode.name)

    fun skalLeseGjennomforingerV2(): Boolean = unleashClient.isEnabled(LES_GJENNOMFORINGER_V2)

    companion object {
        private const val ENABLE_KOMET_DELTAKERE = "amt.enable-komet-deltakere"
        private const val LES_ARENA_DELTAKERE = "amt.les-arena-deltakere"
        private const val LES_GJENNOMFORINGER_V2 = "amt.les-gjennomforing-v2"

        private val tiltakstyperKometErMasterFor = setOf(
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            Tiltakskode.OPPFOLGING,
            Tiltakskode.AVKLARING,
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.JOBBKLUBB,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        )

        private val tiltakstyperKometKanLese = setOf(
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
        )

        private val tiltakstyperKometKanskjeErMasterFor = tiltakstyperKometKanLese
    }
}
