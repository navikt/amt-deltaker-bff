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
        bakgrunnsinformasjon: String?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = BakgrunnsinformasjonRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            bakgrunnsinformasjon = bakgrunnsinformasjon,
        ),
    )

    suspend fun endreInnhold(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        innhold: Deltakelsesinnhold,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = InnholdRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            deltakelsesinnhold = innhold,
        ),
    )

    suspend fun endreDeltakelsesmengde(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        deltakelsesprosent: Float?,
        dagerPerUke: Float?,
        gyldigFra: LocalDate?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = DeltakelsesmengdeRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            deltakelsesprosent = deltakelsesprosent?.toInt(),
            dagerPerUke = dagerPerUke?.toInt(),
            gyldigFra = gyldigFra,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun endreStartdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        startdato: LocalDate?,
        sluttdato: LocalDate?,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = StartdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            startdato = startdato,
            sluttdato = sluttdato,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun endreSluttdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = SluttdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = sluttdato,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun endreSluttaarsak(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = SluttarsakRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = aarsak,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun forlengDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = ForlengDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = sluttdato,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun ikkeAktuell(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = IkkeAktuellRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = aarsak,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun reaktiverDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        begrunnelse: String,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = ReaktiverDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun avbrytDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = AvbrytDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = sluttdato,
            aarsak = aarsak,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun avsluttDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak?,
        begrunnelse: String?,
        harFullfort: Boolean?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = AvsluttDeltakelseRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            sluttdato = sluttdato,
            aarsak = aarsak,
            begrunnelse = begrunnelse,
            harFullfort = harFullfort,
        ),
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
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = EndreAvslutningRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            aarsak = aarsak,
            begrunnelse = begrunnelse,
            sluttdato = sluttdato,
            harFullfort = harFullfort,
        ),
    )

    suspend fun fjernOppstartsdato(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndreDeltaker(
        deltakerId = deltakerId,
        requestBody = FjernOppstartsdatoRequest(
            endretAv = endretAv,
            endretAvEnhet = endretAvEnhet,
            forslagId = forslagId,
            begrunnelse = begrunnelse,
        ),
    )

    suspend fun sistBesokt(deltakerId: UUID, sistBesokt: ZonedDateTime) {
        val response = performPost("deltaker/$deltakerId/$SIST_BESOKT_URL_SEGMENT", sistBesokt)

        if (!response.status.isSuccess()) {
            log.warn(
                "Kunne ikke endre $SIST_BESOKT_URL_SEGMENT i amt-deltaker. Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    private suspend fun postEndreDeltaker(deltakerId: UUID, requestBody: EndringRequest): DeltakerEndringResponse =
        performPost("deltaker/$deltakerId/$ENDRE_DELTAKER_URL_SEGMENT", requestBody)
            .failIfNotSuccess("Kunne ikke oppdatere deltaker $deltakerId med ${requestBody::class.java.simpleName} i amt-deltaker")
            .body()

    companion object Endepunkt {
        const val ENDRE_DELTAKER_URL_SEGMENT = "endre-deltaker"
        const val SIST_BESOKT_URL_SEGMENT = "sist-besokt"
    }
}
