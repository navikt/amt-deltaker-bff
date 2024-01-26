package no.nav.amt.deltaker.bff.deltaker.api.utils

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
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
    fun testValiderBegrunnelse() {
        val forLang = input(MAX_BEGRUNNELSES_LENGDE + 1)
        val ok = input(MAX_BEGRUNNELSES_LENGDE - 1)

        shouldThrow<IllegalArgumentException> {
            validerBegrunnelse(forLang)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerBegrunnelse(ok)
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

    private fun input(n: Int) = (1..n).map { ('a'..'z').random() }.joinToString("")
}
