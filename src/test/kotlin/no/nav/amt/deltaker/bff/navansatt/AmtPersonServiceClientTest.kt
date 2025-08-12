package no.nav.amt.deltaker.bff.navansatt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.navbruker.toDto
import no.nav.amt.deltaker.bff.utils.createMockHttpClient
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavAnsatt
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavBruker
import no.nav.amt.deltaker.bff.utils.data.TestData.lagNavEnhet
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.toDto
import no.nav.amt.lib.ktor.clients.AmtPersonServiceClient
import no.nav.amt.lib.models.person.dto.NavBrukerFodselsarDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Year

class AmtPersonServiceClientTest {
    @Nested
    inner class HentNavAnsattByNavIdent {
        val ansatt = lagNavAnsatt()
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-ansatt"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-ansatt fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansatt.navIdent)
            }
        }

        @Test
        fun `skal returnere NavAnsatt`() {
            runHappyPathTest(expectedUrl, lagNavAnsatt()) { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansatt.navIdent)
            }
        }
    }

    @Nested
    inner class HentNavAnsattById {
        val ansatt = lagNavAnsatt()
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-ansatt/${ansatt.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-ansatt fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansatt.id)
            }
        }

        @Test
        fun `skal returnere NavAnsatt`() {
            runHappyPathTest(expectedUrl, ansatt) { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansatt.id)
            }
        }
    }

    @Nested
    inner class HentNavEnhetByNavEnhetsnummer {
        val navEnhet = lagNavEnhet()
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-enhet"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-enhet fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavEnhet(navEnhet.enhetsnummer)
            }
        }

        @Test
        fun `skal returnere NavEnhet`() {
            runHappyPathTest(expectedUrl, navEnhet, navEnhet.toDto()) { personServiceClient ->
                personServiceClient.hentNavEnhet(navEnhet.enhetsnummer)
            }
        }
    }

    @Nested
    inner class HentNavEnhetById {
        val navEnhet = lagNavEnhet()
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-enhet/${navEnhet.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-enhet fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavEnhet(navEnhet.id)
            }
        }

        @Test
        fun `skal returnere NavEnhet`() {
            runHappyPathTest(expectedUrl, navEnhet, navEnhet.toDto()) { personServiceClient ->
                personServiceClient.hentNavEnhet(navEnhet.id)
            }
        }
    }

    @Nested
    inner class HentNavBruker {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-bruker"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente nav-bruker fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavBruker("~personident~")
            }
        }

        @Test
        fun `skal returnere NavBruker`() {
            val navBrukerDto = lagNavBruker().toDto(lagNavEnhet())
            runHappyPathTest(expectedUrl, navBrukerDto.toModel(), navBrukerDto) { personServiceClient ->
                personServiceClient.hentNavBruker("~personident~")
            }
        }
    }

    @Nested
    inner class HentNavBrukerFodselsar {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-bruker-fodselsar"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(
                expectedUrl,
                "Kunne ikke hente fodselsar for nav-bruker fra amt-person-service",
            ) { personServiceClient ->
                personServiceClient.hentNavBrukerFodselsar("~personident~")
            }
        }

        @Test
        fun `skal returnere fodselsar`() {
            val brukerFodselsarDto = NavBrukerFodselsarDto(Year.now().value - 20)
            runHappyPathTest(expectedUrl, brukerFodselsarDto.fodselsar, brukerFodselsarDto) { personServiceClient ->
                personServiceClient.hentNavBrukerFodselsar("~personident~")
            }
        }
    }

    companion object {
        private const val PERSON_SVC_BASE_URL = "http://amt-person-svc"

        private fun runFailureTest(
            expectedUrl: String,
            expectedError: String,
            block: suspend (AmtPersonServiceClient) -> Unit,
        ) {
            val thrown = runBlocking {
                shouldThrow<RuntimeException> {
                    block(createPersonServiceClient(expectedUrl, HttpStatusCode.Unauthorized))
                }
            }
            thrown.message shouldStartWith expectedError
        }

        private fun <T : Any> runHappyPathTest(
            expectedUrl: String,
            expectedResponse: T,
            responseBody: Any = expectedResponse,
            block: suspend (AmtPersonServiceClient) -> T,
        ) = runBlocking {
            val personServiceClient = createPersonServiceClient(expectedUrl, HttpStatusCode.OK, responseBody)
            block(personServiceClient) shouldBe expectedResponse
        }

        private fun createPersonServiceClient(
            expectedUrl: String,
            statusCode: HttpStatusCode = HttpStatusCode.OK,
            responseBody: Any? = null,
        ) = AmtPersonServiceClient(
            baseUrl = PERSON_SVC_BASE_URL,
            scope = "scope",
            httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
            azureAdTokenClient = mockAzureAdClient(),
        )
    }
}
