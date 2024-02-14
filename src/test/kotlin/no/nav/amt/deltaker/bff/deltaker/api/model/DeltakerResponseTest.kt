package no.nav.amt.deltaker.bff.deltaker.api.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import org.junit.Test

class DeltakerResponseTest {

    val innholdselementer = listOf(
        Innholdselement("Innhold 4", "innhold-4"),
        Innholdselement("Innhold 3", "innhold-3"),
        Innholdselement("Innhold 2", "innhold-2"),
        Innholdselement("Innhold 1", "innhold-1"),
    )

    @Test
    fun `fulltInnhold - ingen innhold er valgt - returner liste med innhold som ikke er valgt og riktig sortert`() {
        val innhold = fulltInnhold(emptyList(), innholdselementer)
        innhold.size shouldBe innholdselementer.size + 1
        innhold.forEach { it.valgt shouldBe false }
        innhold.forEachIndexed { index, innholdelement ->
            if (index == (innhold.size - 1)) {
                innholdelement.tekst shouldBe annetInnholdselement.tekst
            } else {
                innholdelement.tekst shouldBe "Innhold ${index + 1}"
            }
        }
    }

    @Test
    fun `fulltInnhold - noe innhold er valgt - returner liste med innhold som er valgt og ikke er valgt`() {
        val valgtInnhold = listOf(
            innholdselementer.first().toInnhold(valgt = true),
            annetInnholdselement.toInnhold(valgt = true, beskrivelse = "fordi"),
        )

        val innhold = fulltInnhold(valgtInnhold, innholdselementer)
        innhold.size shouldBe innholdselementer.size + 1
        innhold.forEach {
            when (it.innholdskode) {
                valgtInnhold[0].innholdskode -> it.valgt shouldBe true
                valgtInnhold[1].innholdskode -> {
                    it.valgt shouldBe true
                    it.beskrivelse shouldBe valgtInnhold[1].beskrivelse
                }

                else -> it.valgt shouldBe false
            }
        }
    }
}
