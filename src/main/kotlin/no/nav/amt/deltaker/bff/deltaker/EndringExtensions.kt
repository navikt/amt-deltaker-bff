package no.nav.amt.deltaker.bff.deltaker

import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.AvbrytDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.AvsluttDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.BakgrunnsinformasjonRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.DeltakelsesmengdeRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.EndreAvslutningRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.EndringRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.FjernOppstartsdatoRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.ForlengDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.IkkeAktuellRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.InnholdRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.ReaktiverDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.SluttarsakRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.SluttdatoRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.StartdatoRequest
import java.util.UUID

fun DeltakerEndring.Endring.toEndringRequest(
    endretAv: String,
    endretAvEnhet: String,
    forslagId: UUID?,
): EndringRequest = when (this) {
    is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> {
        BakgrunnsinformasjonRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            bakgrunnsinformasjon = this.bakgrunnsinformasjon,
        )
    }

    is DeltakerEndring.Endring.EndreInnhold -> {
        InnholdRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            Deltakelsesinnhold(
                ledetekst = this.ledetekst,
                innhold = this.innhold,
            ),
        )
    }

    is DeltakerEndring.Endring.AvsluttDeltakelse -> {
        AvsluttDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = this.sluttdato,
            aarsak = this.aarsak,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.AvbrytDeltakelse -> {
        AvbrytDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = this.sluttdato,
            aarsak = this.aarsak,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.EndreAvslutning -> {
        EndreAvslutningRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = this.aarsak,
            begrunnelse = this.begrunnelse,
            sluttdato = this.sluttdato,
            harFullfort = this.harFullfort,
        )
    }

    is DeltakerEndring.Endring.EndreDeltakelsesmengde -> {
        DeltakelsesmengdeRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            deltakelsesprosent = this.deltakelsesprosent?.toInt(),
            dagerPerUke = this.dagerPerUke?.toInt(),
            gyldigFra = this.gyldigFra,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.EndreSluttarsak -> {
        SluttarsakRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = this.aarsak,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.EndreSluttdato -> {
        SluttdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = this.sluttdato,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.EndreStartdato -> {
        StartdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            startdato = this.startdato,
            sluttdato = this.sluttdato,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.ForlengDeltakelse -> {
        ForlengDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = this.sluttdato,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.IkkeAktuell -> {
        IkkeAktuellRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = this.aarsak,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.ReaktiverDeltakelse -> {
        ReaktiverDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            begrunnelse = this.begrunnelse,
        )
    }

    is DeltakerEndring.Endring.FjernOppstartsdato -> {
        FjernOppstartsdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            begrunnelse = this.begrunnelse,
        )
    }
}
