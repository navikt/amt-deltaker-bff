package no.nav.amt.deltaker.bff.utils

import java.time.LocalDate

class Personident(
    private val ident: String,
) {
    // I D-Nummer så legges det til 4 i det første sifferet
    private val dnummerOffset = 40
    private val individsiffer = ident.slice(6..8)

    private val fodselsdag = "%02d".format(ident.slice(0..1).toInt() % dnummerOffset)
    private val fodselsmnd = ident.slice(2..3)
    private val fodselsaar = ident.slice(4..5)

    /*
       Individsiffer + fødselsår bestemmer hvilket århundre personer er født i:
        a)	1854–1899 bruker serien 749-500,
        b)	1900–1999 bruker serien 499-000,
        c)	1940–1999 bruker også serien 999-900,
        d)	2000–2039 bruker serien 999-500.
     */
    private val individsifferSerier = mapOf(
        "1854-1899" to "500".."749",
        "1900-1999" to "000".."499",
        "1940-1999" to "900".."999",
        "2000-2039" to "500".."999",
    )

    fun fodselsdato(): LocalDate {
        val aarhundre = when (individsiffer) {
            in individsifferSerier["1854-1899"]!! -> if (fodselsaar in "54".."99") "18" else "20"
            in individsifferSerier["1900-1999"]!! -> "19"
            in individsifferSerier["1940-1999"]!! -> if (fodselsaar in "40".."99") "19" else "20"
            else -> "20"
        }
        val aarstall = aarhundre + fodselsaar

        return LocalDate.of(aarstall.toInt(), fodselsmnd.toInt(), fodselsdag.toInt())
    }

    override fun toString(): String = ident
}
