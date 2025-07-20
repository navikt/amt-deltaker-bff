package no.nav.amt.deltaker.bff.deltaker.amtdistribusjon

import com.github.benmanes.caffeine.cache.Cache
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.utils.CountingCache
import no.nav.amt.deltaker.bff.utils.createMockHttpClient
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import org.junit.jupiter.api.Test

class AmtDistribusjonClientTest {
    @Test
    fun `digitalBruker skal kaste feil nar respons med feilkode`(): Unit = runBlocking {
        val distribusjonClient = createAmtDistribusjonClient(EXPECTED_URL, HttpStatusCode.BadRequest)

        val thrown = shouldThrow<IllegalStateException> {
            distribusjonClient.digitalBruker("~personident~")
        }

        thrown.message shouldStartWith "Kunne ikke hente om bruker er digital fra amt-distribusjon."
    }

    @Test
    fun `digitalBruker skal returnere true`(): Unit = runBlocking {
        val distribusjonClient = createAmtDistribusjonClient(EXPECTED_URL, responseBody = DigitalBrukerResponse(erDigital = true))
        val erDigitalBruker = distribusjonClient.digitalBruker("~personident~")
        erDigitalBruker shouldBe true
    }

    @Test
    fun `digitalBruker skal returnere false`(): Unit = runBlocking {
        val distribusjonClient = createAmtDistribusjonClient(EXPECTED_URL, responseBody = DigitalBrukerResponse(erDigital = false))
        val erDigitalBruker = distribusjonClient.digitalBruker("~personident~")
        erDigitalBruker shouldBe false
    }

    @Test
    fun `skal bruke cache ved andre kall til digitalBruker`(): Unit = runBlocking {
        val countingCache = CountingCache<String, Boolean>()

        val distribusjonClient = createAmtDistribusjonClient(
            expectedUrl = EXPECTED_URL,
            responseBody = DigitalBrukerResponse(erDigital = false),
            cache = countingCache,
        )

        distribusjonClient.digitalBruker("~personident~")
        distribusjonClient.digitalBruker("~personident~")

        countingCache.putCount shouldBe 1
    }

    companion object {
        private fun createAmtDistribusjonClient(
            expectedUrl: String,
            statusCode: HttpStatusCode = HttpStatusCode.OK,
            responseBody: DigitalBrukerResponse? = null,
            cache: Cache<String, Boolean>? = null,
        ) = if (cache == null) {
            AmtDistribusjonClient(
                baseUrl = DISTRIBUSJON_BASE_URL,
                scope = "scope",
                httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
                azureAdTokenClient = mockAzureAdClient(),
            )
        } else {
            AmtDistribusjonClient(
                baseUrl = DISTRIBUSJON_BASE_URL,
                scope = "scope",
                httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
                azureAdTokenClient = mockAzureAdClient(),
                digitalBrukerCache = cache,
            )
        }

        private const val DISTRIBUSJON_BASE_URL = "http://amt-distribusjon"
        private const val EXPECTED_URL = "$DISTRIBUSJON_BASE_URL/digital"
    }
}
