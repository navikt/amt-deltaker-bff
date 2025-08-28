package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.hendelse.Hendelse
import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
import no.nav.amt.lib.models.hendelse.HendelseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UlestHendelse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltakerId: UUID,
    val ansvarlig: HendelseAnsvarlig, // TODO lage egen klasse for ansvarlig og?
    val hendelse: UlestHendelseType,
)

fun Hendelse.toUlestHendelse() = this.payload.toUlestHendelseType()?.let {
    UlestHendelse(
        this.id,
        this.opprettet,
        this.deltaker.id,
        this.ansvarlig,
        it,
    )
}

fun HendelseType.toUlestHendelseType() = when (val hendelseType = this) {
    is HendelseType.InnbyggerGodkjennUtkast -> UlestHendelseType.InnbyggerGodkjennUtkast
    is HendelseType.NavGodkjennUtkast -> UlestHendelseType.NavGodkjennUtkast

    is HendelseType.LeggTilOppstartsdato -> UlestHendelseType.LeggTilOppstartsdato(
        hendelseType.startdato,
        hendelseType.sluttdato,
    )
    is HendelseType.FjernOppstartsdato -> UlestHendelseType.FjernOppstartsdato(
        hendelseType.begrunnelseFraNav,
        hendelseType.begrunnelseFraArrangor,
        hendelseType.endringFraForslag,
    )
    is HendelseType.EndreStartdato -> UlestHendelseType.EndreStartdato(
        hendelseType.startdato,
        hendelseType.sluttdato,
        hendelseType.begrunnelseFraNav,
        hendelseType.begrunnelseFraArrangor,
        hendelseType.endringFraForslag,
    )
    is HendelseType.IkkeAktuell -> UlestHendelseType.IkkeAktuell(
        hendelseType.aarsak,
        hendelseType.begrunnelseFraNav,
        hendelseType.begrunnelseFraArrangor,
        hendelseType.endringFraForslag,
    )
    is HendelseType.AvsluttDeltakelse -> UlestHendelseType.AvsluttDeltakelse(
        hendelseType.aarsak,
        hendelseType.sluttdato,
        hendelseType.begrunnelseFraNav,
        hendelseType.begrunnelseFraArrangor,
        hendelseType.endringFraForslag,
    )
    is HendelseType.AvbrytDeltakelse -> UlestHendelseType.AvbrytDeltakelse(
        hendelseType.aarsak,
        hendelseType.sluttdato,
        hendelseType.begrunnelseFraNav,
        hendelseType.begrunnelseFraArrangor,
        hendelseType.endringFraForslag,
    )
    is HendelseType.ReaktiverDeltakelse -> UlestHendelseType.ReaktiverDeltakelse(
        hendelseType.begrunnelseFraNav,
    )
    else -> null
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface UlestHendelseAnsvarlig {
    data class NavVeileder(
        val id: UUID,
        val navn: String,
        val navIdent: String,
        val enhet: Enhet,
    ) : UlestHendelseAnsvarlig {
        data class Enhet(
            val id: UUID,
            val enhetsnummer: String,
        )
    }

    data class Deltaker(
        val id: UUID,
        val navn: String,
    ) : UlestHendelseAnsvarlig
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface UlestHendelseType {
    sealed interface HendelseMedForslag : UlestHendelseType {
        val begrunnelseFraNav: String?
        val begrunnelseFraArrangor: String?
        val endringFraForslag: Forslag.Endring?
    }

    data object InnbyggerGodkjennUtkast : UlestHendelseType

    data object NavGodkjennUtkast : UlestHendelseType

    data class LeggTilOppstartsdato(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
    ) : UlestHendelseType

    data class FjernOppstartsdato(
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate? = null,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class IkkeAktuell(
        val aarsak: DeltakerEndring.Aarsak,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class AvsluttDeltakelse(
        val aarsak: DeltakerEndring.Aarsak?,
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class AvbrytDeltakelse(
        val aarsak: DeltakerEndring.Aarsak?,
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class ReaktiverDeltakelse(
        val begrunnelseFraNav: String,
    ) : UlestHendelseType
}
