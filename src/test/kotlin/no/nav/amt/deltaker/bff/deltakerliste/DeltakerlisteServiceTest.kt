package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class DeltakerlisteServiceTest {
    private val deltakerlisteService = DeltakerlisteService(DeltakerlisteRepository())

    @Test
    fun `hentDeltakerlisteMedFellesOppstart - deltakerliste har felles oppstart - returnere success`() {
        with(DeltakerlisteContext()) {
            deltakerlisteService.hentMedFellesOppstart(deltakerliste.id).isSuccess shouldBe true
        }
    }

    @Test
    fun `hentDeltakerlisteMedFellesOppstart - deltakerliste har lopende oppstart - returnere failure`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            val result = deltakerlisteService.hentMedFellesOppstart(deltakerliste.id)

            result.isFailure shouldBe true
            result.exceptionOrNull()?.javaClass shouldBe NoSuchElementException::class.java
        }
    }

    @Test
    fun `verifiserDeltakerlisteHarFellesOppstart - deltakerliste har felles oppstart - kaster ikke exception`() {
        with(DeltakerlisteContext()) {
            deltakerlisteService.verifiserDeltakerlisteHarFellesOppstart(deltakerliste.id)
        }
    }

    @Test
    fun `verifiserDeltakerlisteHarFellesOppstart - deltakerliste har lopende oppstart - kaster exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            assertThrows<NoSuchElementException> {
                deltakerlisteService.verifiserDeltakerlisteHarFellesOppstart(deltakerliste.id)
            }
        }
    }
}

data class DeltakerlisteContext(
    val tiltak: Tiltakstype.Tiltakskode = Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    val deltakerliste: Deltakerliste = TestData.lagDeltakerliste(
        tiltak = TestData.lagTiltakstype(tiltakskode = tiltak),
        oppstart = if (tiltak.erKurs()) {
            Deltakerliste.Oppstartstype.FELLES
        } else {
            Deltakerliste.Oppstartstype.LOPENDE
        },
    ),
) {
    init {
        SingletonPostgres16Container
        TestRepository.insert(deltakerliste)
    }
}
