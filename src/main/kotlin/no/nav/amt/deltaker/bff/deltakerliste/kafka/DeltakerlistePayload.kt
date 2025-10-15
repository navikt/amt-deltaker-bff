package no.nav.amt.deltaker.bff.deltakerliste.kafka

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.amt.deltaker.bff.deltakerliste.Deltakerliste
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import java.time.LocalDate
import java.util.UUID

data class DeltakerlistePayload(
    val type: String? = null, // finnes kun for v2, kan fjernes etter overgang til v2
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String? = null, // finnes kun for gruppetiltak
    val startDato: LocalDate? = null, // finnes kun for gruppetiltak
    val sluttDato: LocalDate? = null, // finnes kun for gruppetiltak
    val status: String? = null, // finnes kun for gruppetiltak
    val oppstart: Oppstartstype? = null, // finnes kun for gruppetiltak
    val apentForPamelding: Boolean = true, // finnes kun for gruppetiltak
    val virksomhetsnummer: String? = null, // finnes kun for v1
    val arrangor: Arrangor? = null, // finnes kun for v2
    val antallPlasser: Int? = null, // finnes kun for gruppetiltak
) {
    data class Tiltakstype(
        val tiltakskode: String,
    ) {
        fun erStottet() = Tiltakskode.entries.any { it.name == tiltakskode }
    }

    data class Arrangor(
        val organisasjonsnummer: String,
    )

    @get:JsonIgnore
    val organisasjonsnummer: String
        get() = when {
            type in setOf(
                ENKELTPLASS_V2_TYPE,
                GRUPPE_V2_TYPE,
            ) -> arrangor?.organisasjonsnummer

            else -> virksomhetsnummer
        } ?: throw IllegalStateException("Virksomhetsnummer mangler")

    fun toModel(
        arrangor: no.nav.amt.lib.models.deltaker.Arrangor,
        tiltakstype: no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype,
    ) = Deltakerliste(
        id = this.id,
        tiltak = tiltakstype,
        navn = this.navn ?: tiltakstype.navn,
        status = this.status?.let { Deltakerliste.Status.fromString(it) },
        startDato = this.startDato,
        sluttDato = this.sluttDato,
        oppstart = this.oppstart,
        arrangor = Deltakerliste.Arrangor(
            arrangor = arrangor,
            overordnetArrangorNavn = null,
        ),
        apentForPamelding = this.apentForPamelding,
        antallPlasser = this.antallPlasser,
    )

    companion object {
        const val ENKELTPLASS_V2_TYPE = "TiltaksgjennomforingV2.Enkeltplass"
        const val GRUPPE_V2_TYPE = "TiltaksgjennomforingV2.Gruppe"
    }
}
