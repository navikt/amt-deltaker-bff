package no.nav.amt.deltaker.bff.apiclients.deltaker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import no.nav.amt.lib.ktor.auth.AzureAdTokenClient
import no.nav.amt.lib.ktor.clients.ApiClientBase
import no.nav.amt.lib.ktor.clients.failIfNotSuccess
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.AvsluttDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.BakgrunnsinformasjonRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.DeltakelsesmengdeRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.EndreAvslutningRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.FjernOppstartsdatoRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.ForlengDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.IkkeAktuellRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.InnholdRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.ReaktiverDeltakelseRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.SluttarsakRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.SluttdatoRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.request.StartdatoRequest
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.response.DeltakerEndringResponse
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.response.DeltakerMedStatusResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class AmtDeltakerClient(
    baseUrl: String,
    scope: String,
    httpClient: HttpClient,
    azureAdTokenClient: AzureAdTokenClient,
) : ApiClientBase(
        baseUrl = baseUrl,
        scope = scope,
        httpClient = httpClient,
        azureAdTokenClient = azureAdTokenClient,
    ) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getDeltaker(deltakerId: UUID): DeltakerMedStatusResponse = performGet("deltaker/$deltakerId")
        .failIfNotSuccess("Fant ikke deltaker $deltakerId i amt-deltaker.")
        .body()

    suspend fun endreBakgrunnsinformasjon(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreBakgrunnsinformasjon,
    ) = postEndring(
        deltakerId = deltakerId,
        requestBody = BakgrunnsinformasjonRequest(endretAv, endretAvEnhet, endring.bakgrunnsinformasjon),
        endepunkt = BAKGRUNNSINFORMASJON,
    )

    suspend fun endreInnhold(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        innhold: Deltakelsesinnhold,
    ) = postEndring(deltakerId, InnholdRequest(endretAv, endretAvEnhet, innhold), INNHOLD)

    suspend fun endreDeltakelsesmengde(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        deltakelsesprosent: Float?,
        dagerPerUke: Float?,
        gyldigFra: LocalDate?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        DeltakelsesmengdeRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            deltakelsesprosent = deltakelsesprosent?.toInt(),
            dagerPerUke = dagerPerUke?.toInt(),
            gyldigFra = gyldigFra,
            begrunnelse = begrunnelse,
        ),
        DELTAKELSESMENGDE,
    )

    suspend fun endreStartdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        startdato: LocalDate?,
        sluttdato: LocalDate?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId = deltakerId,
        requestBody = StartdatoRequest(endretAv, endretAvEnhet, forslagId, startdato, sluttdato, begrunnelse),
        endepunkt = STARTDATO,
    )

    suspend fun endreSluttdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(deltakerId, SluttdatoRequest(endretAv, endretAvEnhet, forslagId, sluttdato, begrunnelse), SLUTTDATO)

    suspend fun endreSluttaarsak(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(deltakerId, SluttarsakRequest(endretAv, endretAvEnhet, forslagId, aarsak, begrunnelse), SLUTTARSAK)

    suspend fun forlengDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        ForlengDeltakelseRequest(endretAv, endretAvEnhet, forslagId, sluttdato, begrunnelse),
        FORLENG_DELTAKELSE,
    )

    suspend fun ikkeAktuell(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        IkkeAktuellRequest(endretAv, endretAvEnhet, forslagId, aarsak, begrunnelse),
        IKKE_AKTUELL,
    )

    suspend fun reaktiverDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        begrunnelse: String,
    ) = postEndring(deltakerId, ReaktiverDeltakelseRequest(endretAv, endretAvEnhet, begrunnelse), REAKTIVER)

    suspend fun avbrytDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        AvsluttDeltakelseRequest(endretAv, endretAvEnhet, forslagId, sluttdato, aarsak, begrunnelse),
        AVBRYT_DELTAKELSE,
    )

    suspend fun avsluttDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        AvsluttDeltakelseRequest(endretAv, endretAvEnhet, forslagId, sluttdato, aarsak, begrunnelse),
        AVSLUTT_DELTAKELSE,
    )

    suspend fun endreAvslutning(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak?,
        begrunnelse: String?,
        harFullfort: Boolean?,
        sluttdato: LocalDate?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        EndreAvslutningRequest(endretAv, endretAvEnhet, forslagId, aarsak, begrunnelse, sluttdato, harFullfort),
        ENDRE_AVSLUTNING,
    )

    suspend fun fjernOppstartsdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        FjernOppstartsdatoRequest(endretAv, endretAvEnhet, forslagId, begrunnelse),
        FJERN_OPPSTARTSDATO,
    )

    suspend fun sistBesokt(deltakerId: UUID, sistBesokt: ZonedDateTime) {
        val response = performPost("deltaker/$deltakerId/$SIST_BESOKT", sistBesokt)

        if (!response.status.isSuccess()) {
            log.warn("Kunne ikke endre $SIST_BESOKT i amt-deltaker. Status=${response.status.value} error=${response.bodyAsText()}")
        }
    }

    private suspend fun postEndring(
        deltakerId: UUID,
        requestBody: Any,
        endepunkt: String,
    ): DeltakerEndringResponse = performPost("deltaker/$deltakerId/$endepunkt", requestBody)
        .failIfNotSuccess("Kunne ikke endre $endepunkt i amt-deltaker.")
        .body()

    companion object Endepunkt {
        const val BAKGRUNNSINFORMASJON = "bakgrunnsinformasjon"
        const val INNHOLD = "innhold"
        const val DELTAKELSESMENGDE = "deltakelsesmengde"
        const val STARTDATO = "startdato"
        const val SLUTTDATO = "sluttdato"
        const val SLUTTARSAK = "sluttarsak"
        const val FORLENG_DELTAKELSE = "forleng"
        const val IKKE_AKTUELL = "ikke-aktuell"
        const val AVSLUTT_DELTAKELSE = "avslutt"
        const val ENDRE_AVSLUTNING = "endre-avslutning"
        const val AVBRYT_DELTAKELSE = "avbryt"
        const val SIST_BESOKT = "sist-besokt"
        const val REAKTIVER = "reaktiver"
        const val FJERN_OPPSTARTSDATO = "fjern-oppstartsdato"
    }
}
