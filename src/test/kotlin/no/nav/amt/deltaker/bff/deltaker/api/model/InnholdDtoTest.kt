package no.nav.amt.deltaker.bff.deltaker.api.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test

class InnholdDtoTest {
    @Test
    fun testFinnValgtInnhold() {
        val innholdselement = Innholdselement("Type", "type")
        val deltaker = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(
                    innhold = TestData.lagDeltakerRegistreringInnhold(
                        innholdselementer = listOf(innholdselement, annetInnholdselement),
                    ),
                ),
            ),
        )

        val annetBeskrivelse = "annet må ha en beskrivelse"

        val valgtInnhold = finnValgtInnhold(
            innhold = listOf(
                InnholdDto(innholdselement.innholdskode, null),
                InnholdDto(annetInnholdselement.innholdskode, annetBeskrivelse),
            ),
            deltaker = deltaker,
        )
        valgtInnhold shouldBe listOf(
            innholdselement.toInnhold(true),
            annetInnholdselement.toInnhold(true, annetBeskrivelse),
        )
    }
}
