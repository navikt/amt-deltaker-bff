package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model

import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
import no.nav.amt.lib.models.hendelse.HendelseType
import java.time.LocalDateTime
import java.util.UUID

data class UlestHendelse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltakerId: UUID,
    val ansvarlig: HendelseAnsvarlig,
    val hendelse: HendelseType,
)
