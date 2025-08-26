package no.nav.amt.deltaker.bff.apiclients.deltaker

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.utils.createMockHttpClient
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.lagDeltaker
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.toDeltakerEndringResponse
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.response.DeltakerEndringResponse
import no.nav.amt.lib.models.deltaker.internalapis.deltaker.response.DeltakerMedStatusResponse
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
    inner class GetDeltaker {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}"
        val expectedErrorMessage = "Fant ikke deltaker ${deltakerInTest.id} i amt-deltaker."
        val getDeltakerLambda: suspend (AmtDeltakerClient) -> DeltakerMedStatusResponse =
            { client -> client.getDeltaker(deltakerInTest.id) }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
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
        val endreBakgrunnsinformasjonLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
            { client ->
                client.endreBakgrunnsinformasjon(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    endring = DeltakerEndring.Endring.EndreBakgrunnsinformasjon("~bakgrunnsinformasjon~"),
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreBakgrunnsinformasjonLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerEndringResponseInTest,
                block = endreBakgrunnsinformasjonLambda,
            )
        }
    }

    @Nested
    inner class EndreInnhold {
        val innhold = Deltakelsesinnhold(null, emptyList())
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/innhold"
        val expectedErrorMessage = "Kunne ikke endre innhold i amt-deltaker."
        val endreInnholdLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
            { client ->
                client.endreInnhold(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    innhold = innhold,
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreInnholdLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerEndringResponseInTest,
                block = endreInnholdLambda,
            )
        }
    }

    @Nested
    inner class EndreDeltakelsesmengde {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/deltakelsesmengde"
        val expectedErrorMessage = "Kunne ikke endre deltakelsesmengde i amt-deltaker."
        val endreDeltakelsesmengdeLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreDeltakelsesmengdeLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerEndringResponseInTest,
                block = endreDeltakelsesmengdeLambda,
            )
        }
    }

    @Nested
    inner class EndreStartdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/startdato"
        val expectedErrorMessage = "Kunne ikke endre startdato i amt-deltaker."
        val endreStartdatoLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreStartdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(
                expectedUrl = expectedUrl,
                expectedResponse = deltakerEndringResponseInTest,
                block = endreStartdatoLambda,
            )
        }
    }

    @Nested
    inner class EndreSluttdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/sluttdato"
        val expectedErrorMessage = "Kunne ikke endre sluttdato i amt-deltaker."
        val endreSluttdatoLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreSluttdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerEndringResponseInTest, endreSluttdatoLambda)
        }
    }

    @Nested
    inner class EndreSluttaarsak {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/sluttarsak"
        val expectedErrorMessage = "Kunne ikke endre sluttarsak i amt-deltaker."
        val endreSluttaarsakLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, endreSluttaarsakLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerEndringResponseInTest, endreSluttaarsakLambda)
        }
    }

    @Nested
    inner class ForlengDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/forleng"
        val expectedErrorMessage = "Kunne ikke endre forleng i amt-deltaker."
        val forlengDeltakelseLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, forlengDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl = expectedUrl, expectedResponse = deltakerEndringResponseInTest, forlengDeltakelseLambda)
        }
    }

    @Nested
    inner class IkkeAktuell {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/ikke-aktuell"
        val expectedErrorMessage = "Kunne ikke endre ikke-aktuell i amt-deltaker."
        val ikkeAktuellLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, ikkeAktuellLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerEndringResponseInTest, ikkeAktuellLambda)
        }
    }

    @Nested
    inner class ReaktiverDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/reaktiver"
        val expectedErrorMessage = "Kunne ikke endre reaktiver i amt-deltaker."
        val reaktiverDeltakelseLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
            { client ->
                client.reaktiverDeltakelse(
                    deltakerId = deltakerInTest.id,
                    endretAv = "~endretAv~",
                    endretAvEnhet = "~endretAvEnhet~",
                    begrunnelse = "~begrunnelse~",
                )
            }

        @ParameterizedTest
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, reaktiverDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerEndringResponseInTest, reaktiverDeltakelseLambda)
        }
    }

    @Nested
    inner class AvsluttDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/avslutt"
        val expectedErrorMessage = "Kunne ikke endre avslutt i amt-deltaker."
        val avsluttDeltakelseLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, avsluttDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerEndringResponseInTest, avsluttDeltakelseLambda)
        }
    }

    @Nested
    inner class AvbrytDeltakelse {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/avbryt"
        val expectedErrorMessage = "Kunne ikke endre avbryt i amt-deltaker."
        val avbrytDeltakelseLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, avbrytDeltakelseLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerEndringResponseInTest, avbrytDeltakelseLambda)
        }
    }

    @Nested
    inner class FjernOppstartsdato {
        val expectedUrl = "$DELTAKER_BASE_URL/deltaker/${deltakerInTest.id}/fjern-oppstartsdato"
        val expectedErrorMessage = "Kunne ikke endre fjern-oppstartsdato i amt-deltaker."
        val fjernOppstartsdatoLambda: suspend (AmtDeltakerClient) -> DeltakerEndringResponse =
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
        @MethodSource("no.nav.amt.deltaker.bff.apiclients.ApiClientTestUtils#failureCases")
        fun `skal kaste riktig exception ved feilrespons`(testCase: Pair<HttpStatusCode, KClass<out Throwable>>) {
            val (statusCode, expectedExceptionType) = testCase
            runFailureTest(expectedExceptionType, statusCode, expectedUrl, expectedErrorMessage, fjernOppstartsdatoLambda)
        }

        @Test
        fun `skal returnere Deltakeroppdatering`() {
            runHappyPathTest(expectedUrl, deltakerEndringResponseInTest, fjernOppstartsdatoLambda)
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
        private val deltakerInTest = lagDeltaker()
        private val deltakerEndringResponseInTest = deltakerInTest.toDeltakerEndringResponse()

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
