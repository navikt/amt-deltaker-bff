package no.nav.amt.deltaker.bff.deltakerliste

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.navansatt.AmtPersonServiceClient
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DeltakerlisteServiceTest {
    private val amtPersonServiceClient = mockk<AmtPersonServiceClient>()
    private val deltakerlisteService = DeltakerlisteService(DeltakerlisteRepository(), amtPersonServiceClient)

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
    fun `verifiserTilgjengeligDeltakerliste - deltakerliste har felles oppstart - kaster ikke exception`() {
        with(DeltakerlisteContext()) {
            deltakerlisteService.verifiserTilgjengeligDeltakerliste(deltakerliste.id)
        }
    }

    @Test
    fun `verifiserTilgjengeligDeltakerliste - deltakerliste har lopende oppstart - kaster exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
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

    @Test
    fun `sjekkAldersgrenseForDeltakelse - deltaker for ung grufagyrke - kaster exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)) {
            medStartdato(LocalDate.of(2025, 1, 1))
            val personident = "01010750042"

            val fodselsar = LocalDate.now().year - 17
            coEvery { amtPersonServiceClient.hentNavBrukerFodselsar(personident) } returns fodselsar

            assertThrows<DeltakerForUngException> {
                runBlocking {
                    deltakerlisteService.sjekkAldersgrenseForDeltakelse(deltakerliste.id, personident)
                }
            }
        }
    }

    @Test
    fun `sjekkAldersgrenseForDeltakelse - deltaker for ung gruamo - kaster exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING)) {
            medStartdato(LocalDate.of(2025, 1, 1))
            val personident = "01010750042"
            val fodselsar = LocalDate.now().year - 17
            coEvery { amtPersonServiceClient.hentNavBrukerFodselsar(personident) } returns fodselsar

            assertThrows<DeltakerForUngException> {
                runBlocking {
                    deltakerlisteService.sjekkAldersgrenseForDeltakelse(deltakerliste.id, personident)
                }
            }
        }
    }

    @Test
    fun `sjekkAldersgrenseForDeltakelse - deltaker ikke for ung grufagyrke - kaster ikke exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)) {
            medStartdato(LocalDate.of(2025, 1, 1))
            val personident = "01010650042"
            val fodselsar = LocalDate.now().year - 27
            coEvery { amtPersonServiceClient.hentNavBrukerFodselsar(personident) } returns fodselsar

            assertDoesNotThrow {
                runBlocking {
                    deltakerlisteService.sjekkAldersgrenseForDeltakelse(deltakerliste.id, personident)
                }
            }
        }
    }

    @Test
    fun `sjekkAldersgrenseForDeltakelse - deltaker ikke for ung gruamo - kaster ikke exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING)) {
            medStartdato(LocalDate.of(2025, 1, 1))
            val personident = "01010650042"
            val fodselsar = LocalDate.now().year - 27
            coEvery { amtPersonServiceClient.hentNavBrukerFodselsar(personident) } returns fodselsar

            assertDoesNotThrow {
                runBlocking {
                    deltakerlisteService.sjekkAldersgrenseForDeltakelse(deltakerliste.id, personident)
                }
            }
        }
    }

    @Test
    fun `sjekkAldersgrenseForDeltakelse - ikke grufagyrke - kaster ikke exception`() {
        with(DeltakerlisteContext(Tiltakstype.Tiltakskode.JOBBKLUBB)) {
            medStartdato(LocalDate.of(2025, 1, 1))
            val personident = "01010750042"
            val fodselsar = LocalDate.now().year - 17
            coEvery { amtPersonServiceClient.hentNavBrukerFodselsar(personident) } returns fodselsar

            assertDoesNotThrow {
                runBlocking {
                    deltakerlisteService.sjekkAldersgrenseForDeltakelse(deltakerliste.id, personident)
                }
            }
        }
    }
}

data class DeltakerlisteContext(
    val tiltak: Tiltakstype.Tiltakskode = Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    var deltakerliste: Deltakerliste = TestData.lagDeltakerliste(
        tiltak = TestData.lagTiltakstype(tiltakskode = tiltak),
        oppstart = if (tiltak.erKurs()) {
            Deltakerliste.Oppstartstype.FELLES
        } else {
            Deltakerliste.Oppstartstype.LOPENDE
        },
    ),
) {
    val repository = DeltakerlisteRepository()

    init {
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

    fun medStartdato(dato: LocalDate) {
        deltakerliste = deltakerliste.copy(startDato = dato, sluttDato = dato.plusMonths(6))
        repository.upsert(deltakerliste)
    }
}
