package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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
        with(DeltakerlisteContext(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            val result = deltakerlisteService.hentMedFellesOppstart(deltakerliste.id)

            result.isFailure shouldBe true
            result.exceptionOrNull()?.javaClass shouldBe NoSuchElementException::class.java
        }
    }

    @Test
    fun `verifiserTilgjengeligDeltakerliste - deltakerliste har felles oppstart - kaster ikke exception`() {
        with(DeltakerlisteContext()) {
            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id)
        }
    }

    @Test
    fun `verifiserTilgjengeligDeltakerliste - deltakerliste har lopende oppstart - kaster exception`() {
        with(DeltakerlisteContext(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            assertThrows<NoSuchElementException> {
                deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id)
            }
        }
    }

    @Test
    fun `verifiserTilgjengeligDeltakerliste - deltakerlistes sluttdato og graceperiode er passert - kaster exception`() {
        with(DeltakerlisteContext()) {
            medAvsluttetDeltakerliste()
            assertThrows<DeltakerlisteStengtException> {
                deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id)
            }
        }
    }

    @Test
    fun `verifiserTilgjengeligDeltakerliste - deltakerlistes sluttdato er ikke passert - kaster ikke exception`() {
        with(DeltakerlisteContext()) {
            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id)
        }
    }
}

data class DeltakerlisteContext(
    val tiltak: Tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    var deltakerliste: Deltakerliste = TestData.lagDeltakerliste(
        tiltak = TestData.lagTiltakstype(tiltakskode = tiltak),
        oppstart = if (tiltak in setOf(
                Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
                Tiltakskode.JOBBKLUBB,
            )
        ) {
            Deltakerliste.Oppstartstype.FELLES
        } else {
            Deltakerliste.Oppstartstype.LOPENDE
        },
    ),
) {
    val repository = DeltakerlisteRepository()

    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container

        TestRepository.insert(deltakerliste)
    }

    fun medAvsluttetDeltakerliste() {
        deltakerliste = deltakerliste.copy(
            status = Deltakerliste.Status.AVSLUTTET,
            startDato = LocalDate.now().minusMonths(3),
            sluttDato = LocalDate.now().minus(DeltakerlisteService.tiltakskoordinatorGraceperiode).minusDays(1),
        )

        repository.upsert(deltakerliste)
    }
}
