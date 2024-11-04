package no.nav.amt.deltaker.bff.deltaker.api.model

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltaker.model.Innsatsgruppe
import no.nav.amt.deltaker.bff.deltaker.model.months
import no.nav.amt.deltaker.bff.deltaker.model.weeks
import no.nav.amt.deltaker.bff.deltaker.model.years
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test
import java.time.LocalDate

class DeltakerTest {
    @Test
    fun `maxVarighetDato - skal kalkulere riktig max varighet basert tiltakstype`() {
        val deltakere = Tiltakstype.Tiltakskode.entries.map {
            TestData.lagDeltaker(
                startdato = LocalDate.now(),
                deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = it)),
            )
        }

        deltakere.forEach {
            when (it.deltakerliste.tiltak.tiltakskode) {
                Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                -> it.maxVarighet shouldBe years(3)

                Tiltakstype.Tiltakskode.AVKLARING,
                Tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                -> it.maxVarighet shouldBe weeks(16)

                Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> it.maxVarighet shouldBe weeks(12)
                Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> it.maxVarighet shouldBe years(4)

                Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                Tiltakstype.Tiltakskode.JOBBKLUBB,
                -> it.maxVarighet shouldBe null

                Tiltakstype.Tiltakskode.OPPFOLGING -> when (it.navBruker.innsatsgruppe) {
                    Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> it.maxVarighet shouldBe years(1)
                    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    -> it.maxVarighet shouldBe years(6).plus(months(6))

                    else -> it.maxVarighet shouldBe null
                }
            }
        }
    }

    @Test
    fun `maxVarighetDato - skal kalkulere riktig max varighet basert på innsatsgruppe for oppfølging`() {
        val deltakerStandardInnsats = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING)),
            navBruker = TestData.lagNavBruker(innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS),
        )
        val deltakerSituasjonsbestemt = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING)),
            navBruker = TestData.lagNavBruker(innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS),
        )
        val andreInnsatsgrupper = listOf(
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS,
        ).map {
            TestData.lagDeltaker(
                deltakerliste = TestData.lagDeltakerliste(
                    tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING),
                ),
                navBruker = TestData.lagNavBruker(innsatsgruppe = it),
            )
        }

        deltakerStandardInnsats.maxVarighet shouldBe null
        deltakerSituasjonsbestemt.maxVarighet shouldBe years(1)
        andreInnsatsgrupper.forEach { it.maxVarighet shouldBe years(3).plus(months(6)) }
    }

    @Test
    fun `softVarighetDato - skal kalkulere riktig varighet basert på innsatsgruppe for oppfølging`() {
        val deltakerStandardInnsats = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING)),
            navBruker = TestData.lagNavBruker(innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS),
        )
        val deltakerSituasjonsbestemt = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING)),
            navBruker = TestData.lagNavBruker(innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS),
        )
        val andreInnsatsgrupper = listOf(
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS,
        ).map {
            TestData.lagDeltaker(
                deltakerliste = TestData.lagDeltakerliste(
                    tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.OPPFOLGING),
                ),
                navBruker = TestData.lagNavBruker(innsatsgruppe = it),
            )
        }

        deltakerStandardInnsats.softMaxVarighet shouldBe null
        deltakerSituasjonsbestemt.softMaxVarighet shouldBe null
        andreInnsatsgrupper.forEach { it.softMaxVarighet shouldBe years(3) }
    }

    @Test
    fun `softMaxVarighetDato - skal kalkulere riktig varighet basert tiltakstype`() {
        val deltakere = Tiltakstype.Tiltakskode.entries.map {
            TestData.lagDeltaker(
                startdato = LocalDate.now(),
                deltakerliste = TestData.lagDeltakerliste(tiltak = TestData.lagTiltakstype(tiltakskode = it)),
            )
        }

        deltakere.forEach {
            when (it.deltakerliste.tiltak.tiltakskode) {
                Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> it.softMaxVarighet shouldBe years(2)
                Tiltakstype.Tiltakskode.OPPFOLGING -> when (it.navBruker.innsatsgruppe) {
                    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                    -> it.maxVarighet shouldBe years(3)

                    else -> it.maxVarighet shouldBe null
                }

                Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
                -> it.softMaxVarighet shouldBe weeks(8)
                Tiltakstype.Tiltakskode.AVKLARING,
                Tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                -> it.softMaxVarighet shouldBe weeks(12)
                Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
                Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                Tiltakstype.Tiltakskode.JOBBKLUBB,
                Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                -> it.softMaxVarighet shouldBe null
            }
        }
    }
}
