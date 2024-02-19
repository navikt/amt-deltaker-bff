package no.nav.amt.deltaker.bff.deltaker.api.utils

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test

class InputvalideringTest {
    @Test
    fun testValiderBakgrunnsinformasjon() {
        val forLang = input(MAX_BAKGRUNNSINFORMASJON_LENGDE + 1)
        val ok = input(MAX_BAKGRUNNSINFORMASJON_LENGDE - 1)

        shouldThrow<IllegalArgumentException> {
            validerBakgrunnsinformasjon(forLang)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerBakgrunnsinformasjon(ok)
        }
    }

    @Test
    fun testValiderAnnetInnhold() {
        val forLang = input(MAX_ANNET_INNHOLD_LENGDE + 1)
        val ok = input(MAX_ANNET_INNHOLD_LENGDE - 1)

        shouldThrow<IllegalArgumentException> {
            validerAnnetInnhold(forLang)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerAnnetInnhold(ok)
        }
    }

    @Test
    fun testValiderAarsaksBeskrivelse() {
        val forLang = input(MAX_AARSAK_BESKRIVELSE_LENGDE + 1)
        val ok = input(MAX_AARSAK_BESKRIVELSE_LENGDE - 1)

        shouldThrow<IllegalArgumentException> {
            validerAarsaksBeskrivelse(forLang)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerAarsaksBeskrivelse(ok)
        }
    }

    @Test
    fun testValiderDagerPerUke() {
        shouldThrow<IllegalArgumentException> {
            validerDagerPerUke(MIN_DAGER_PER_UKE - 1)
        }
        shouldThrow<IllegalArgumentException> {
            validerDagerPerUke(MAX_DAGER_PER_UKE + 1)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDagerPerUke(MIN_DAGER_PER_UKE)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDagerPerUke(MAX_DAGER_PER_UKE)
        }
    }

    @Test
    fun testValiderDeltakelsesProsent() {
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesProsent(MIN_DELTAKELSESPROSENT - 1)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesProsent(MAX_DELTAKELSESPROSENT + 1)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesProsent(MIN_DELTAKELSESPROSENT)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesProsent(MAX_DELTAKELSESPROSENT)
        }
    }

    @Test
    fun testValiderInnhold() {
        val tiltaksinnhold = TestData.lagDeltakerRegistreringInnhold(
            innholdselementer = listOf(
                Innholdselement("Type", "type"),
                annetInnholdselement,
            ),
        )

        shouldThrow<IllegalArgumentException> {
            validerInnhold(emptyList(), null)
        }
        shouldThrow<IllegalArgumentException> {
            validerInnhold(listOf(InnholdDto("foo", null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerInnhold(listOf(InnholdDto(annetInnholdselement.innholdskode, null)), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerInnhold(
                listOf(InnholdDto(annetInnholdselement.innholdskode, "annet innhold må ha beskrivelse")),
                tiltaksinnhold,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerInnhold(listOf(InnholdDto("type", null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerInnhold(listOf(InnholdDto("type", "andre typer enn annet skal ikke ha beskrivelse")), tiltaksinnhold)
        }
    }

    private fun input(n: Int) = (1..n).map { ('a'..'z').random() }.joinToString("")
}
