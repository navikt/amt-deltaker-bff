package no.nav.amt.deltaker.bff.arrangor

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.mockAzureAdClient
import no.nav.amt.deltaker.bff.utils.mockHttpClient
import org.junit.Test

class AmtArrangorClientTest {
    @Test
    fun `hentArrangor(orgnr) - skal parse response riktig og returnere arrangor`(): Unit = runBlocking {
        val overordnetArrangor = TestData.lagArrangor()
        val arrangor = TestData.lagArrangor(overordnetArrangorId = overordnetArrangor.id)

        val arrangorDto = ArrangorDto(arrangor.id, arrangor.navn, arrangor.organisasjonsnummer, overordnetArrangor)

        val httpClient = mockHttpClient(objectMapper.writeValueAsString(arrangorDto))

        val amtArrangorClient = AmtArrangorClient(
            baseUrl = "http://amt-arrangor",
            scope = "scope",
            httpClient = httpClient,
            azureAdTokenClient = mockAzureAdClient(),
        )

        amtArrangorClient.hentArrangor(arrangor.organisasjonsnummer) shouldBe arrangorDto
    }

    @Test
    fun `hentArrangor(id) - skal parse response riktig og returnere arrangor`(): Unit = runBlocking {
        val overordnetArrangor = TestData.lagArrangor()
        val arrangor = TestData.lagArrangor(overordnetArrangorId = overordnetArrangor.id)

        val arrangorDto = ArrangorDto(arrangor.id, arrangor.navn, arrangor.organisasjonsnummer, overordnetArrangor)

        val httpClient = mockHttpClient(objectMapper.writeValueAsString(arrangorDto))

        val amtArrangorClient = AmtArrangorClient(
            baseUrl = "http://amt-arrangor",
            scope = "scope",
            httpClient = httpClient,
            azureAdTokenClient = mockAzureAdClient(),
        )

        amtArrangorClient.hentArrangor(arrangor.id) shouldBe arrangorDto
    }
}
