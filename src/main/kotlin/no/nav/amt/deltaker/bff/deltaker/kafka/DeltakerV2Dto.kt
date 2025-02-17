package no.nav.amt.deltaker.bff.deltaker.kafka

import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.navbruker.model.NavBruker
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerV2Dto(
    val id: UUID,
    val deltakerlisteId: UUID,
    val personalia: DeltakerPersonaliaDto,
    val status: DeltakerStatusDto,
    val dagerPerUke: Float?,
    val prosentStilling: Double?,
    val oppstartsdato: LocalDate?,
    val sluttdato: LocalDate?,
    val bestillingTekst: String?,
    val kilde: Kilde?,
    val innhold: Deltakelsesinnhold?,
    val historikk: List<DeltakerHistorikk>?,
    val vurderingerFraArrangor: List<Vurdering>?,
    val forcedUpdate: Boolean? = false,
    val sistEndret: LocalDateTime?,
) {
    fun toDeltakerOppdatering(): Deltakeroppdatering {
        require(status.id != null) { "Kan ikke håndtere deltakerstatus uten id for deltaker $id" }

        return Deltakeroppdatering(
            id = id,
            startdato = oppstartsdato,
            sluttdato = sluttdato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = prosentStilling?.toFloat(),
            bakgrunnsinformasjon = bestillingTekst,
            deltakelsesinnhold = innhold,
            status = DeltakerStatus(
                id = status.id,
                type = status.type,
                aarsak = status.aarsak?.let { DeltakerStatus.Aarsak(it, status.aarsaksbeskrivelse) },
                gyldigFra = status.gyldigFra,
                gyldigTil = null,
                opprettet = status.opprettetDato,
            ),
            historikk = historikk.orEmpty(),
            sistEndret = sistEndret ?: LocalDateTime.now(),
            forcedUpdate = forcedUpdate,
        )
    }

    fun toDeltaker(navBruker: NavBruker, deltakerliste: Deltakerliste) = Deltaker(
        id = id,
        navBruker = navBruker,
        deltakerliste = deltakerliste,
        startdato = oppstartsdato,
        sluttdato = sluttdato,
        dagerPerUke = dagerPerUke,
        deltakelsesprosent = prosentStilling?.toFloat(),
        bakgrunnsinformasjon = bestillingTekst,
        deltakelsesinnhold = innhold,
        status = DeltakerStatus(
            id = status.id ?: throw IllegalStateException("deltakerstatus mangler id $id"),
            type = status.type,
            aarsak = status.aarsak?.let { DeltakerStatus.Aarsak(it, status.aarsaksbeskrivelse) },
            gyldigFra = status.gyldigFra,
            gyldigTil = null,
            opprettet = status.opprettetDato,
        ),
        historikk = historikk.orEmpty(),
        kanEndres = true,
        sistEndret = sistEndret ?: LocalDateTime.now(),
    )

    enum class Kilde {
        KOMET,
        ARENA,
    }

    data class DeltakerPersonaliaDto(
        val personident: String,
    )

    data class DeltakerStatusDto(
        val id: UUID?,
        val type: DeltakerStatus.Type,
        val aarsak: DeltakerStatus.Aarsak.Type?,
        val aarsaksbeskrivelse: String?,
        val gyldigFra: LocalDateTime,
        val opprettetDato: LocalDateTime,
    )
}
