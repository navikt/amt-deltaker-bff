package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model

import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
import java.time.LocalDateTime
import java.util.UUID

data class UlestHendelse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltakerId: UUID,
    val ansvarlig: HendelseAnsvarlig, // TODO lage egen klasse for ansvarlig og?
    val hendelse: UlestHendelseType,
)
