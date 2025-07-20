package no.nav.amt.deltaker.bff.deltaker.amtdeltaker

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.amtdeltaker.response.DeltakerMedStatusResponse
import no.nav.amt.deltaker.bff.deltaker.api.model.toKladdResponse
import no.nav.amt.deltaker.bff.deltaker.toDeltakeroppdateringResponse
import no.nav.amt.deltaker.bff.testdata.OpprettTestDeltakelseRequest
import no.nav.amt.deltaker.bff.testdata.TestdataService.Companion.lagUtkast
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.AvslagRequest
import no.nav.amt.deltaker.bff.utils.createMockHttpClient
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerStatus
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltakerliste
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavBruker
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.toDeltakeroppdatering
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class AmtDeltakerClientTest {
    @Nested
    inner class TildelPlass {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/tildel-plass"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke tildele plass i amt-deltaker") { deltakerClient ->
                deltakerClient.tildelPlass(listOf(deltaker.id), "~endretAv~")
            }
        }

        @Test
        fun `skal returnere DeltakerOppdateringResponse liste`() {
            val expectedResponse = listOf(deltaker.toDeltakeroppdateringResponse())

            runHappyPathTest(expectedUrl, expectedResponse) { deltakerClient ->
                deltakerClient.tildelPlass(listOf(deltaker.id), "~endretAv~")
            }
        }
    }

    @Nested
    inner class SettPaaVenteliste {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/sett-paa-venteliste"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke sette på venteliste i amt-deltaker.") { deltakerClient ->
                deltakerClient.settPaaVenteliste(emptyList(), "~endretAv~")
            }
        }

        @Test
        fun `skal returnere DeltakerOppdateringResponse liste`() {
            val deltaker = lagDeltaker()
            val expectedResponse = listOf(deltaker.toDeltakeroppdateringResponse())

            runHappyPathTest(expectedUrl, expectedResponse) { deltakerClient ->
                deltakerClient.settPaaVenteliste(listOf(deltaker.id), "~endretAv~")
            }
        }
    }

    @Nested
    inner class OpprettKladd {
        val expectedUrl = "$DELTAKER_BASE_URL/pamelding"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke opprette kladd i amt-deltaker.") { deltakerClient ->
                deltakerClient.opprettKladd(UUID.randomUUID(), "~personident~")
            }
        }

        @Test
        fun `skal returnere KladdResponse`() {
            val deltaker = lagDeltaker()
            val expectedResponse = deltaker.toKladdResponse()

            runHappyPathTest(expectedUrl, expectedResponse) { deltakerClient ->
                deltakerClient.opprettKladd(UUID.randomUUID(), "~personident~")
            }
        }
    }

    @Nested
    inner class Utkast {
        val expectedUrl = "$DELTAKER_BASE_URL/pamelding/${deltaker.id}"

        val navBruker = lagNavBruker(deltaker.id, navEnhetId = UUID.randomUUID())
        val deltakerListe = lagDeltakerliste()

        val opprettTestDeltakelseRequest = OpprettTestDeltakelseRequest(
            personident = navBruker.personident,
            deltakerlisteId = deltakerListe.id,
            startdato = LocalDate.now().minusDays(1),
            deltakelsesprosent = 60,
            dagerPerUke = 3,
        )

        val utkast = lagUtkast(deltaker.id, deltakerListe, opprettTestDeltakelseRequest)

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke oppdatere utkast i amt-deltaker.") { deltakerClient ->
                deltakerClient.utkast(utkast)
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            val expectedResponse = deltaker.toDeltakeroppdatering()

            runHappyPathTest(expectedUrl, expectedResponse) { deltakerClient ->
                deltakerClient.utkast(utkast)
            }
        }
    }

    @Nested
    inner class SlettKladd {
        val expectedUrl = "$DELTAKER_BASE_URL/pamelding/${deltaker.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke slette kladd i amt-deltaker.") { deltakerClient ->
                deltakerClient.slettKladd(deltaker.id)
            }
        }

        @Test
        fun `skal slette kladd uten feil`() {
            runHappyPathTest(expectedUrl, null) { deltakerClient ->
                deltakerClient.slettKladd(deltaker.id)
            }
        }
    }

    @Nested
    inner class AvbrytUtkast {
        val expectedUrl = "$DELTAKER_BASE_URL/pamelding/${deltaker.id}/avbryt"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke avbryte utkast i amt-deltaker.") { deltakerClient ->
                deltakerClient.avbrytUtkast(deltaker.id, "~avbruttAv~", "~avbruttAvEnhet~")
            }
        }

        @Test
        fun `skal avbryte utkast uten feil`() {
            runHappyPathTest(expectedUrl, null) { deltakerClient ->
                deltakerClient.avbrytUtkast(deltaker.id, "~avbruttAv~", "~avbruttAvEnhet~")
            }
        }
    }

    @Nested
    inner class GetDeltaker {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Fant ikke deltaker ${deltaker.id} i amt-deltaker.") { deltakerClient ->
                deltakerClient.getDeltaker(deltaker.id)
            }
        }

        @Test
        fun `skal returnere DeltakerMedStatusResponse`() {
            val expectedResponse = DeltakerMedStatusResponse(deltaker.id, lagDeltakerStatus())

            runHappyPathTest(expectedUrl, expectedResponse) { deltakerClient ->
                deltakerClient.getDeltaker(deltaker.id)
            }
        }
    }

    @Nested
    inner class EndreBakgrunnsinformasjon {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/bakgrunnsinformasjon"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre bakgrunnsinformasjon i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreBakgrunnsinformasjon(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreBakgrunnsinformasjon(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
                )
            }
        }
    }

    @Nested
    inner class EndreInnhold {
        val innhold = Deltakelsesinnhold(null, emptyList())
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/innhold"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre innhold i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreInnhold(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    innhold = innhold,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreInnhold(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    innhold = innhold,
                )
            }
        }
    }

    @Nested
    inner class EndreDeltakelsesmengde {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/deltakelsesmengde"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre deltakelsesmengde i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreDeltakelsesmengde(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                    null,
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreDeltakelsesmengde(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                    null,
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class EndreStartdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/startdato"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre startdato i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreStartdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreStartdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class EndreSluttdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/sluttdato"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre sluttdato i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreSluttdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreSluttdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class EndreSluttaarsak {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/sluttarsak"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre sluttarsak i amt-deltaker.") { deltakerClient ->
                deltakerClient.endreSluttaarsak(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.endreSluttaarsak(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class ForlengDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/forleng"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre forleng i amt-deltaker.") { deltakerClient ->
                deltakerClient.forlengDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.forlengDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class IkkeAktuell {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/ikke-aktuell"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre ikke-aktuell i amt-deltaker.") { deltakerClient ->
                deltakerClient.ikkeAktuell(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.ikkeAktuell(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class ReaktiverDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/reaktiver"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre reaktiver i amt-deltaker.") { deltakerClient ->
                deltakerClient.reaktiverDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    "~begrunnelse~",
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.reaktiverDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    "~begrunnelse~",
                )
            }
        }
    }

    @Nested
    inner class AvsluttDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/avslutt"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre avslutt i amt-deltaker.") { deltakerClient ->
                deltakerClient.avsluttDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.avsluttDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class AvbrytDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/avbryt"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre avbryt i amt-deltaker.") { deltakerClient ->
                deltakerClient.avbrytDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.avbrytDeltakelse(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    LocalDate.now(),
                    DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class FjernOppstartsdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/fjern-oppstartsdato"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke endre fjern-oppstartsdato i amt-deltaker.") { deltakerClient ->
                deltakerClient.fjernOppstartsdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                )
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.fjernOppstartsdato(
                    deltakerId = deltaker.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    null,
                    null,
                )
            }
        }
    }

    @Nested
    inner class InnbyggerGodkjennUtkast {
        val expectedUrl = "$DELTAKER_BASE_URL/pamelding/${deltaker.id}/innbygger/godkjenn-utkast"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke fatte vedtak i amt-deltaker.") { deltakerClient ->
                deltakerClient.innbyggerGodkjennUtkast(deltaker.id)
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.innbyggerGodkjennUtkast(deltaker.id)
            }
        }
    }

    @Nested
    inner class DelMedArrangor {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/del-med-arrangor"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke dele-med-arrangor i amt-deltaker.") { deltakerClient ->
                deltakerClient.delMedArrangor(listOf(deltaker.id), "~endretAv~")
            }
        }

        @Test
        fun `skal returnere liste med DeltakeroppdateringResponse`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = listOf(deltaker.toDeltakeroppdateringResponse()),
            ) { deltakerClient ->
                deltakerClient.delMedArrangor(listOf(deltaker.id), "~endretAv~")
            }
        }
    }

    @Nested
    inner class GiAvslag {
        val avslagRequest = AvslagRequest(
            deltakerId = deltaker.id,
            EndringFraTiltakskoordinator.Avslag.Aarsak(EndringFraTiltakskoordinator.Avslag.Aarsak.Type.ANNET, null),
            null,
        )
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/gi-avslag"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke gi avslag i amt-deltaker.") { deltakerClient ->
                deltakerClient.giAvslag(avslagRequest, "~endretAv~")
            }
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltaker.toDeltakeroppdatering()) { deltakerClient ->
                deltakerClient.giAvslag(avslagRequest, "~endretAv~")
            }
        }
    }

    @Nested
    inner class SistBesokt {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltaker.id}/sist-besokt"

        @Test
        fun `skal logge warning ved feil`() {
            val deltakerClient = createDeltakerClient(expectedUrl, HttpStatusCode.Unauthorized)

            withLogCapture("no.nav.amt.deltaker.bff.deltaker.amtdeltaker.AmtDeltakerClient") { logEvents ->
                deltakerClient.sistBesokt(deltaker.id, ZonedDateTime.now())

                val logEntry = logEvents.find { it.level.levelStr == "WARN" }
                logEntry.shouldNotBeNull()
                logEntry.message shouldStartWith "Kunne ikke endre sist-besokt i amt-deltaker"
            }
        }

        @Test
        fun `skal ikke kaste feil nar sistBesokt kalles`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = null,
            ) { deltakerClient ->
                deltakerClient.sistBesokt(deltaker.id, ZonedDateTime.now())
            }
        }
    }

    companion object {
        private const val DELTAKER_BASE_URL = "http://amt-deltaker"
        private val deltaker = lagDeltaker()

        private fun runFailureTest(
            expectedUrl: String,
            expectedError: String,
            block: suspend (AmtDeltakerClient) -> Unit,
        ) {
            val thrown = runBlocking {
                shouldThrow<IllegalStateException> {
                    block(createDeltakerClient(expectedUrl, HttpStatusCode.Unauthorized))
                }
            }
            thrown.message shouldStartWith expectedError
        }

        private fun <T> runHappyPathTest(
            expectedUrl: String,
            expectedResponse: T,
            block: suspend (AmtDeltakerClient) -> T,
        ) = runBlocking {
            val deltakerClient = createDeltakerClient(expectedUrl, HttpStatusCode.OK, expectedResponse)

            if (expectedResponse == null) {
                shouldNotThrowAny { block(deltakerClient) }
            } else {
                block(deltakerClient) shouldBe expectedResponse
            }
        }

        private fun createDeltakerClient(
            expectedUrl: String,
            statusCode: HttpStatusCode = HttpStatusCode.OK,
            responseBody: Any? = null,
        ) = AmtDeltakerClient(
            baseUrl = DELTAKER_BASE_URL,
            scope = "scope",
            httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
            azureAdTokenClient = mockAzureAdClient(),
        )
    }
}
