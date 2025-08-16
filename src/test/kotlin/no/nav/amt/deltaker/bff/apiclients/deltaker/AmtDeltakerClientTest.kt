package no.nav.amt.deltaker.bff.apiclients.deltaker

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.apiclients.deltaker.response.DeltakerMedStatusResponse
import no.nav.amt.deltaker.bff.apiclients.deltaker.response.DeltakerOppdateringResponse
import no.nav.amt.deltaker.bff.auth.exceptions.AuthenticationException
import no.nav.amt.deltaker.bff.auth.exceptions.AuthorizationException
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.tiltakskoordinator.api.AvslagRequest
import no.nav.amt.deltaker.bff.utils.createMockHttpClient
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.toDeltakeroppdatering
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class AmtDeltakerClientTest {
    @Nested
    inner class TildelPlass {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/tildel-plass"
        val expectedErrorMessage = "Kunne ikke tildele plass i amt-deltaker"
        val tildelPlassLambda: suspend (AmtDeltakerClient) -> List<DeltakerOppdateringResponse> =
            { client -> client.tildelPlass(listOf(deltakerInTest.id), "~endretAv~") }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, tildelPlassLambda)
        }

        @Test
        fun `skal returnere DeltakerOppdateringResponse liste`() {
            val expectedResponse = listOf(DeltakerOppdateringResponse.fromDeltaker(deltakerInTest))
            runHappyPathTest(expectedUrl, expectedResponse, tildelPlassLambda)
        }
    }

    @Nested
    inner class SettPaaVenteliste {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/sett-paa-venteliste"
        val expectedErrorMessage = "Kunne ikke sette på venteliste i amt-deltaker."
        val settPaaVentelisteLambda: suspend (AmtDeltakerClient) -> List<DeltakerOppdateringResponse> =
            { client -> client.settPaaVenteliste(deltakerIder = listOf(deltakerInTest.id), endretAv = "~endretAv~") }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, settPaaVentelisteLambda)
        }

        @Test
        fun `skal returnere DeltakerOppdateringResponse liste`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = listOf(DeltakerOppdateringResponse.fromDeltaker(deltakerInTest)),
                block = settPaaVentelisteLambda,
            )
        }
    }

    @Nested
    inner class GetDeltaker {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}"
        val expectedErrorMessage = "Fant ikke deltaker ${deltakerInTest.id} i amt-deltaker."
        val getDeltakerLambda: suspend (AmtDeltakerClient) -> DeltakerMedStatusResponse =
            { client -> client.getDeltaker(deltakerInTest.id) }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, getDeltakerLambda)
        }

        @Test
        fun `skal returnere DeltakerMedStatusResponse`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = DeltakerMedStatusResponse(deltakerInTest.id, TestData.lagDeltakerStatus()),
                block = getDeltakerLambda,
            )
        }
    }

    @Nested
    inner class EndreBakgrunnsinformasjon {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/bakgrunnsinformasjon"
        val expectedErrorMessage = "Kunne ikke endre bakgrunnsinformasjon i amt-deltaker."
        val endreBakgrunnsinformasjonLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreBakgrunnsinformasjon(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("~bakgrunnsinformasjon~"),
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreBakgrunnsinformasjonLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerOppdateringInTest,
                block = endreBakgrunnsinformasjonLambda,
            )
        }
    }

    @Nested
    inner class EndreInnhold {
        val innhold = Deltakelsesinnhold(null, emptyList())
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/innhold"
        val expectedErrorMessage = "Kunne ikke endre innhold i amt-deltaker."
        val endreInnholdLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreInnhold(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    innhold = innhold,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreInnholdLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerOppdateringInTest,
                block = endreInnholdLambda,
            )
        }
    }

    @Nested
    inner class EndreDeltakelsesmengde {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/deltakelsesmengde"
        val expectedErrorMessage = "Kunne ikke endre deltakelsesmengde i amt-deltaker."
        val endreDeltakelsesmengdeLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreDeltakelsesmengde(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    deltakelsesprosent = null,
                    dagerPerUke = null,
                    gyldigFra = null,
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreDeltakelsesmengdeLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerOppdateringInTest,
                block = endreDeltakelsesmengdeLambda,
            )
        }
    }

    @Nested
    inner class EndreStartdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/startdato"
        val expectedErrorMessage = "Kunne ikke endre startdato i amt-deltaker."
        val endreStartdatoLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreStartdato(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    startdato = null,
                    sluttdato = null,
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreStartdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerOppdateringInTest,
                block = endreStartdatoLambda,
            )
        }
    }

    @Nested
    inner class EndreSluttdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/sluttdato"
        val expectedErrorMessage = "Kunne ikke endre sluttdato i amt-deltaker."
        val endreSluttdatoLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreSluttdato(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    sluttdato = LocalDate.now(),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreSluttdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerOppdateringInTest, endreSluttdatoLambda)
        }
    }

    @Nested
    inner class EndreSluttaarsak {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/sluttarsak"
        val expectedErrorMessage = "Kunne ikke endre sluttarsak i amt-deltaker."
        val endreSluttaarsakLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.endreSluttaarsak(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    aarsak = DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreSluttaarsakLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerOppdateringInTest, endreSluttaarsakLambda)
        }
    }

    @Nested
    inner class ForlengDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/forleng"
        val expectedErrorMessage = "Kunne ikke endre forleng i amt-deltaker."
        val forlengDeltakelseLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.forlengDeltakelse(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    sluttdato = LocalDate.now(),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, forlengDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerOppdateringInTest, forlengDeltakelseLambda)
        }
    }

    @Nested
    inner class IkkeAktuell {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/ikke-aktuell"
        val expectedErrorMessage = "Kunne ikke endre ikke-aktuell i amt-deltaker."
        val ikkeAktuellLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.ikkeAktuell(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    aarsak = DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, ikkeAktuellLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, ikkeAktuellLambda)
        }
    }

    @Nested
    inner class ReaktiverDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/reaktiver"
        val expectedErrorMessage = "Kunne ikke endre reaktiver i amt-deltaker."
        val reaktiverDeltakelseLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.reaktiverDeltakelse(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    begrunnelse = "~begrunnelse~",
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, reaktiverDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, reaktiverDeltakelseLambda)
        }
    }

    @Nested
    inner class AvsluttDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/avslutt"
        val expectedErrorMessage = "Kunne ikke endre avslutt i amt-deltaker."
        val avsluttDeltakelseLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.avsluttDeltakelse(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    sluttdato = LocalDate.now(),
                    aarsak = DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, avsluttDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, avsluttDeltakelseLambda)
        }
    }

    @Nested
    inner class AvbrytDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/avbryt"
        val expectedErrorMessage = "Kunne ikke endre avbryt i amt-deltaker."
        val avbrytDeltakelseLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.avbrytDeltakelse(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    sluttdato = LocalDate.now(),
                    aarsak = DeltakerEndring.Aarsak(
                        DeltakerEndring.Aarsak.Type.ANNET,
                        "~beskrivelse~",
                    ),
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, avbrytDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, avbrytDeltakelseLambda)
        }
    }

    @Nested
    inner class FjernOppstartsdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/fjern-oppstartsdato"
        val expectedErrorMessage = "Kunne ikke endre fjern-oppstartsdato i amt-deltaker."
        val fjernOppstartsdatoLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.fjernOppstartsdato(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    begrunnelse = null,
                    forslagId = null,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, fjernOppstartsdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, fjernOppstartsdatoLambda)
        }
    }

    @Nested
    inner class DelMedArrangor {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/del-med-arrangor"
        val expectedErrorMessage = "Kunne ikke dele-med-arrangor i amt-deltaker."
        val delMedArrangorLambda: suspend (AmtDeltakerClient) -> List<DeltakerOppdateringResponse> =
            { client ->
                client.delMedArrangor(
                    deltakerIder = listOf(deltakerInTest.id),
                    endretAv = "~endretAv~",
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, delMedArrangorLambda)
        }

        @Test
        fun `skal returnere liste med DeltakeroppdateringResponse`() {
            runHappyPathTest(
                expectedUrl,
                listOf(DeltakerOppdateringResponse.fromDeltaker(deltakerInTest)),
                delMedArrangorLambda,
            )
        }
    }

    @Nested
    inner class GiAvslag {
        val expectedUrl = "$DELTAKER_BASE_URL/tiltakskoordinator/deltakere/gi-avslag"
        val expectedErrorMessage = "Kunne ikke gi avslag i amt-deltaker."
        val avslagRequest = AvslagRequest(
            deltakerId = deltakerInTest.id,
            EndringFraTiltakskoordinator.Avslag.Aarsak(EndringFraTiltakskoordinator.Avslag.Aarsak.Type.ANNET, null),
            null,
        )
        val giAvslagLambda: suspend (AmtDeltakerClient) -> Deltakeroppdatering =
            { client ->
                client.giAvslag(
                    avslagRequest,
                    endretAv = "~endretAv~",
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClientTest#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, giAvslagLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerOppdateringInTest, giAvslagLambda)
        }
    }

    @Nested
    inner class SistBesokt {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/sist-besokt"

        @Test
        fun `skal logge warning ved feil`() {
            val deltakerClient = createDeltakerClient(expectedUrl, HttpStatusCode.Unauthorized)

            withLogCapture("no.nav.amt.deltaker.bff.apiclients.deltaker.AmtDeltakerClient") { logEvents ->
                deltakerClient.sistBesokt(deltakerInTest.id, ZonedDateTime.now())

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
                deltakerClient.sistBesokt(deltakerInTest.id, ZonedDateTime.now())
            }
        }
    }

    companion object {
        private const val DELTAKER_BASE_URL = "http://amt-deltaker"
        private val deltakerInTest = TestData.lagDeltaker()
        private val deltakerOppdateringInTest = deltakerInTest.toDeltakeroppdatering()

        @JvmStatic
        fun failureCases() = listOf(
            Pair(HttpStatusCode.Unauthorized, AuthenticationException::class),
            Pair(HttpStatusCode.Forbidden, AuthorizationException::class),
            Pair(HttpStatusCode.BadRequest, IllegalArgumentException::class),
            Pair(HttpStatusCode.NotFound, NoSuchElementException::class),
            Pair(HttpStatusCode.InternalServerError, IllegalStateException::class),
        )

        private fun runFailureTest(
            exceptionType: KClass<out Throwable>,
            statusCode: HttpStatusCode,
            expectedUrl: String,
            expectedError: String,
            block: suspend (AmtDeltakerClient) -> Any,
        ) {
            val thrown = Assertions.assertThrows(exceptionType.java) {
                runBlocking {
                    block(createDeltakerClient(expectedUrl, statusCode))
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
