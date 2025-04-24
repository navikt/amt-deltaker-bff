package no.nav.amt.deltaker.bff.deltaker.amtdeltaker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import no.nav.amt.deltaker.bff.auth.AzureAdTokenClient
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.AvbrytUtkastRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.AvsluttDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.BakgrunnsinformasjonRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.DeltakelsesmengdeRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.FjernOppstartsdatoRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.ForlengDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.IkkeAktuellRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.InnholdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.OpprettKladdRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.ReaktiverDeltakelseRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.SluttarsakRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.SluttdatoRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.StartdatoRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.request.UtkastRequest
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.DeltakerMedStatusResponse
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.KladdResponse
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.deltaker.model.Utkast
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.tiltakskoordinator.requests.DelMedArrangorRequest
import no.nav.amt.lib.models.tiltakskoordinator.response.EndringFraTiltakskoordinatorResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class AmtDeltakerClient(
    private val baseUrl: String,
    private val scope: String,
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun settPaaVenteliste(deltakerIder: List<UUID>, endretAv: String): List<Deltakeroppdatering> {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/tiltakskoordinator/deltakere/sett-paa-venteliste") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(DeltakereRequest(deltakerIder, endretAv))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke opprette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }

    suspend fun opprettKladd(deltakerlisteId: UUID, personident: String): KladdResponse {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(OpprettKladdRequest(deltakerlisteId, personident))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke opprette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }

    suspend fun utkast(utkast: Utkast): Deltakeroppdatering {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding/${utkast.deltakerId}") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(utkast.toRequest())
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke oppdatere utkast i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }

        return response.body()
    }

    suspend fun slettKladd(deltakerId: UUID) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.delete("$baseUrl/pamelding/$deltakerId") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke slette kladd i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    suspend fun avbrytUtkast(
        deltakerId: UUID,
        avbruttAv: String,
        avbruttAvEnhet: String,
    ) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding/$deltakerId/avbryt") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(AvbrytUtkastRequest(avbruttAv, avbruttAvEnhet))
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke avbryte utkast i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
    }

    suspend fun getDeltaker(deltakerId: UUID): DeltakerMedStatusResponse {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.get("$baseUrl/deltaker/$deltakerId") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        if (!response.status.isSuccess()) {
            error(
                "Fant ikke deltaker $deltakerId i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }

    suspend fun endreBakgrunnsinformasjon(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        endring: DeltakerEndring.Endring.EndreBakgrunnsinformasjon,
    ) = postEndring(
        deltakerId = deltakerId,
        request = BakgrunnsinformasjonRequest(endretAv, endretAvEnhet, endring.bakgrunnsinformasjon),
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
    ) = postEndring(deltakerId, StartdatoRequest(endretAv, endretAvEnhet, forslagId, startdato, sluttdato, begrunnelse), STARTDATO)

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
    ) = postEndring(deltakerId, ForlengDeltakelseRequest(endretAv, endretAvEnhet, forslagId, sluttdato, begrunnelse), FORLENG_DELTAKELSE)

    suspend fun ikkeAktuell(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(deltakerId, IkkeAktuellRequest(endretAv, endretAvEnhet, forslagId, aarsak, begrunnelse), IKKE_AKTUELL)

    suspend fun reaktiverDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        begrunnelse: String,
    ) = postEndring(deltakerId, ReaktiverDeltakelseRequest(endretAv, endretAvEnhet, begrunnelse), REAKTIVER)

    suspend fun avsluttDeltakelse(
        deltakerId: UUID,
        endretAv: String,
        endretAvEnhet: String,
        sluttdato: LocalDate,
        aarsak: DeltakerEndring.Aarsak,
        begrunnelse: String?,
        forslagId: UUID?,
    ) = postEndring(
        deltakerId,
        AvsluttDeltakelseRequest(endretAv, endretAvEnhet, forslagId, sluttdato, aarsak, begrunnelse),
        AVSLUTT_DELTAKELSE,
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

    suspend fun innbyggerGodkjennUtkast(deltakerId: UUID): Deltakeroppdatering {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/pamelding/$deltakerId/innbygger/godkjenn-utkast") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke fatte vedtak i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }

        return response.body()
    }

    suspend fun delMedArrangor(deltakerIder: List<UUID>, endretAv: String): List<EndringFraTiltakskoordinatorResponse> {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val request = DelMedArrangorRequest(endretAv, deltakerIder)
        val response = httpClient.post("$baseUrl/tiltakskoordinator/deltakere/del-med-arrangor") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke dele-med-arrangor i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }

        return response.body()
    }

    private suspend fun postEndring(
        deltakerId: UUID,
        request: Any,
        endepunkt: String,
    ): Deltakeroppdatering {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/deltaker/$deltakerId/$endepunkt") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke endre $endepunkt i amt-deltaker. " +
                    "Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }

        return response.body()
    }

    suspend fun sistBesokt(deltakerId: UUID, sistBesokt: ZonedDateTime) {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$baseUrl/deltaker/$deltakerId/$SIST_BESOKT") {
            header(HttpHeaders.Authorization, token)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(sistBesokt)
        }
        if (!response.status.isSuccess()) {
            log.warn("Kunne ikke endre $SIST_BESOKT i amt-deltaker. Status=${response.status.value} error=${response.bodyAsText()}")
        }
    }

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
        const val SIST_BESOKT = "sist-besokt"
        const val REAKTIVER = "reaktiver"
        const val FJERN_OPPSTARTSDATO = "fjern-oppstartsdato"
    }
}

data class DeltakereRequest(
    val deltakere: List<UUID>,
    val endretAv: String,
)

private fun Utkast.toRequest() = UtkastRequest(
    deltakelsesinnhold = this.pamelding.deltakelsesinnhold,
    bakgrunnsinformasjon = this.pamelding.bakgrunnsinformasjon,
    deltakelsesprosent = this.pamelding.deltakelsesprosent,
    dagerPerUke = this.pamelding.dagerPerUke,
    endretAv = this.pamelding.endretAv,
    endretAvEnhet = this.pamelding.endretAvEnhet,
    godkjentAvNav = this.godkjentAvNav,
)
