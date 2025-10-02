package no.nav.amt.deltaker.bff.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode

class UnleashToggle(
    private val unleashClient: Unleash,
) {
    private val tiltakstyperKometErMasterFor = listOf(
        ArenaKode.ARBFORB,
        ArenaKode.INDOPPFAG,
        ArenaKode.AVKLARAG,
        ArenaKode.ARBRRHDAG,
        ArenaKode.DIGIOPPARB,
        ArenaKode.VASV,
    )

    private val tiltakstyperKometKanLese = listOf(
        ArenaKode.GRUPPEAMO,
        ArenaKode.JOBBK,
        ArenaKode.GRUFAGYRKE,
    )

    private val tiltakstyperKometSkalLese = emptyList<ArenaKode>()

    private val tiltakstyperKometKanskjeErMasterFor = tiltakstyperKometSkalLese

    fun erKometMasterForTiltakstype(tiltakstype: ArenaKode): Boolean = tiltakstype in tiltakstyperKometErMasterFor ||
        (unleashClient.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)

    fun skalLeseArenaDeltakereForTiltakstype(tiltakstype: ArenaKode): Boolean =
        unleashClient.isEnabled("amt.les-arena-deltakere") && tiltakstype in tiltakstyperKometKanLese
}
