package no.nav.amt.deltaker.bff.unleash

import io.getunleash.Unleash
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype

class UnleashToggle(
    private val unleashClient: Unleash,
) {
    private val tiltakstyperKometErMasterFor = listOf(
        Tiltakstype.ArenaKode.ARBFORB,
        Tiltakstype.ArenaKode.INDOPPFAG,
        Tiltakstype.ArenaKode.AVKLARAG,
        Tiltakstype.ArenaKode.ARBRRHDAG,
    )

    // her kan vi legge inn de neste tiltakstypene vi skal ta over
    private val tiltakstyperKometKanskjeErMasterFor = listOf(
        Tiltakstype.ArenaKode.DIGIOPPARB,
        Tiltakstype.ArenaKode.VASV,
    )

    fun erKometMasterForTiltakstype(tiltakstype: Tiltakstype.ArenaKode): Boolean {
        return tiltakstype in tiltakstyperKometErMasterFor ||
            (unleashClient.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)
    }
}
