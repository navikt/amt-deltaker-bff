package no.nav.amt.deltaker.bff.deltaker.api.utils

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.amt.deltaker.bff.deltaker.api.model.InnholdDto
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Innholdselement
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.annetInnholdselement
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestData.input
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
    fun testValiderDeltakelsesinnhold() {
        val tiltaksinnhold = TestData.lagDeltakerRegistreringInnhold(
            innholdselementer = listOf(
                Innholdselement("Type", "type"),
            ),
        )

        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(listOf(InnholdDto("type", null)), null)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(
                listOf(InnholdDto("type", null)),
                TestData.lagDeltakerRegistreringInnhold(innholdselementer = emptyList()),
            )
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(emptyList(), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(emptyList(), null)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(listOf(InnholdDto("foo", null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(listOf(InnholdDto(annetInnholdselement.innholdskode, null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(listOf(InnholdDto(annetInnholdselement.innholdskode, "")), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(
                listOf(InnholdDto(annetInnholdselement.innholdskode, "annet innhold må ha beskrivelse")),
                tiltaksinnhold,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(listOf(InnholdDto("type", null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(
                listOf(InnholdDto("type", "andre typer enn annet skal ikke ha beskrivelse")),
                tiltaksinnhold,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(
                listOf(InnholdDto(annetInnholdselement.innholdskode, "annet er tillatt for tiltak uten innholdselementer")),
                DeltakerRegistreringInnhold(emptyList(), "Ledetekst"),
            )
        }
    }

    @Test
    fun testValiderKladdInnhold() {
        val tiltaksinnhold = TestData.lagDeltakerRegistreringInnhold(
            innholdselementer = listOf(
                Innholdselement("Type", "type"),
                annetInnholdselement,
            ),
        )

        shouldThrow<IllegalArgumentException> {
            validerKladdInnhold(listOf(InnholdDto("type", null)), null)
        }
        shouldThrow<IllegalArgumentException> {
            validerKladdInnhold(
                listOf(InnholdDto("type", null)),
                TestData.lagDeltakerRegistreringInnhold(innholdselementer = emptyList()),
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(emptyList(), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(emptyList(), null)
        }
        shouldThrow<IllegalArgumentException> {
            validerKladdInnhold(listOf(InnholdDto("foo", null)), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(listOf(InnholdDto(annetInnholdselement.innholdskode, null)), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(listOf(InnholdDto(annetInnholdselement.innholdskode, "")), tiltaksinnhold)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(
                listOf(InnholdDto(annetInnholdselement.innholdskode, "annet innhold må ha beskrivelse")),
                tiltaksinnhold,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerKladdInnhold(listOf(InnholdDto("type", null)), tiltaksinnhold)
        }
        shouldThrow<IllegalArgumentException> {
            validerKladdInnhold(
                listOf(InnholdDto("type", "andre typer enn annet skal ikke ha beskrivelse")),
                tiltaksinnhold,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakelsesinnhold(
                listOf(InnholdDto(annetInnholdselement.innholdskode, "annet er tillatt for tiltak uten innholdselementer")),
                DeltakerRegistreringInnhold(emptyList(), "Ledetekst"),
            )
        }
    }

    @Test
    fun testValiderDeltakerKanEndres() {
        val deltakerDeltar = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.DELTAR,
                gyldigFra = LocalDateTime.now(),
            ),
        )
        val deltakerSluttetFireUkerSiden = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                gyldigFra = LocalDateTime.now().minusWeeks(4),
            ),
        )
        val deltakerSluttetFireMndSiden = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                gyldigFra = LocalDateTime.now().minusMonths(4),
            ),
        )
        val deltakerIkkeAktuellFireMndSiden = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.IKKE_AKTUELL,
                gyldigFra = LocalDateTime.now().minusMonths(4),
            ),
        )

        shouldNotThrow<IllegalArgumentException> {
            validerDeltakerKanEndres(deltakerDeltar)
        }
        shouldNotThrow<IllegalArgumentException> {
            validerDeltakerKanEndres(deltakerSluttetFireUkerSiden)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakerKanEndres(deltakerSluttetFireMndSiden)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakerKanEndres(deltakerIkkeAktuellFireMndSiden)
        }
        shouldThrow<IllegalArgumentException> {
            validerDeltakerKanEndres(deltakerSluttetFireUkerSiden.copy(kanEndres = false))
        }
    }

    @Test
    fun testValiderSluttdatoForDeltaker() {
        val deltaker = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(
                startDato = LocalDate.now().minusYears(2),
                sluttDato = LocalDate.now().plusYears(1),
            ),
        )

        shouldNotThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(
                startdato = LocalDate.now().minusDays(10),
                sluttdato = LocalDate.now(),
                opprinneligDeltaker = deltaker,
            )
        }
        shouldNotThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(startdato = null, sluttdato = LocalDate.now(), opprinneligDeltaker = deltaker)
        }
        shouldThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(
                startdato = LocalDate.now().minusDays(10),
                sluttdato = LocalDate.now().minusDays(12),
                opprinneligDeltaker = deltaker,
            )
        }
        shouldThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(
                startdato = LocalDate.now().minusDays(10),
                sluttdato = LocalDate.now().plusYears(2),
                opprinneligDeltaker = deltaker,
            )
        }
    }

    @Test
    fun `validerSluttdato - skal feile hvis sluttdato er utenfor max varighet`() {
        val deltaker = TestData.lagDeltaker(
            deltakerliste = TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK),
            ),
        )

        shouldNotThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(
                startdato = LocalDate.now(),
                sluttdato = LocalDate.now().plusWeeks(12),
                opprinneligDeltaker = deltaker,
            )
        }

        shouldThrow<IllegalArgumentException> {
            validerSluttdatoForDeltaker(
                startdato = LocalDate.now(),
                sluttdato = LocalDate.now().plusWeeks(12).plusDays(1),
                opprinneligDeltaker = deltaker,
            )
        }
    }
}
