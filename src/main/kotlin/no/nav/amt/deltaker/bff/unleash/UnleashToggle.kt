package no.nav.amt.deltaker.bff.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype

class UnleashToggle(
    private val unleashClient: Unleash,
) {
    private val tiltakstyperKometErMasterFor = listOf(
        Tiltakstype.ArenaKode.ARBFORB,
        Tiltakstype.ArenaKode.INDOPPFAG,
        Tiltakstype.ArenaKode.AVKLARAG,
        Tiltakstype.ArenaKode.ARBRRHDAG,
        Tiltakstype.ArenaKode.DIGIOPPARB,
        Tiltakstype.ArenaKode.VASV,
        Tiltakstype.ArenaKode.GRUPPEAMO,
        Tiltakstype.ArenaKode.JOBBK,
        Tiltakstype.ArenaKode.GRUFAGYRKE,
    )

    private val tiltakstyperKometSkalLese = emptyList<Tiltakstype.ArenaKode>()

    private val tiltakstyperKometKanskjeErMasterFor = tiltakstyperKometSkalLese

    fun erKometMasterForTiltakstype(tiltakstype: Tiltakstype.ArenaKode): Boolean = tiltakstype in tiltakstyperKometErMasterFor ||
        (unleashClient.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)

    fun skalLeseArenaDeltakereForTiltakstype(tiltakstype: Tiltakstype.ArenaKode): Boolean =
        unleashClient.isEnabled("amt.les-arena-deltakere") && tiltakstype in tiltakstyperKometSkalLese
}
