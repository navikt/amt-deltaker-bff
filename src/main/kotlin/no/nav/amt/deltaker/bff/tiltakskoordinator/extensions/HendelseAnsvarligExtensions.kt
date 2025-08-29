package no.nav.amt.deltaker.bff.tiltakskoordinator.extensions

import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.AnsvarligNavnOgEnhet
import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig

// TODO: Finne ut av hvor verdier skal hentes fra
fun HendelseAnsvarlig.toAnsvarligNavnOgEnhet(): AnsvarligNavnOgEnhet = when (this) {
    is HendelseAnsvarlig.NavTiltakskoordinator -> AnsvarligNavnOgEnhet(navn, enhet.navn)
    is HendelseAnsvarlig.NavVeileder -> AnsvarligNavnOgEnhet(navn)
    is HendelseAnsvarlig.Deltaker -> AnsvarligNavnOgEnhet()
    is HendelseAnsvarlig.Arrangor -> AnsvarligNavnOgEnhet()
    is HendelseAnsvarlig.System -> AnsvarligNavnOgEnhet()
}
