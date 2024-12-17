package no.nav.amt.deltaker.bff.deltaker

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.deltaker.db.DeltakerRepository
import no.nav.amt.deltaker.bff.deltaker.model.Deltakeroppdatering
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetRepository
import no.nav.amt.deltaker.bff.navansatt.navenhet.NavEnhetService
import no.nav.amt.deltaker.bff.utils.MockResponseHandler
import no.nav.amt.deltaker.bff.utils.data.TestData
import no.nav.amt.deltaker.bff.utils.data.TestRepository
import no.nav.amt.deltaker.bff.utils.data.endre
import no.nav.amt.deltaker.bff.utils.mockAmtDeltakerClient
import no.nav.amt.deltaker.bff.utils.mockAmtPersonServiceClient
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Innhold
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.Test
import java.time.LocalDate

class DeltakerServiceTest {
    init {
        SingletonPostgres16Container
    }

    private val navEnhetService = NavEnhetService(NavEnhetRepository(), mockAmtPersonServiceClient())
    private val service = DeltakerService(DeltakerRepository(), mockAmtDeltakerClient(), navEnhetService, mockk())

    @Test
    fun `oppdaterDeltaker(endring) - kaller client og returnerer deltaker`(): Unit = runBlocking {
        val deltaker = TestData.lagDeltaker()
        TestRepository.insert(deltaker)

        val endringer = listOf(
            DeltakerEndring.Endring.EndreBakgrunnsinformasjon("foo"),
            DeltakerEndring.Endring.EndreInnhold("ledetekst", listOf(Innhold("tekst,", "innholdskode,", true, "beskrivelse"))),
            DeltakerEndring.Endring.EndreDeltakelsesmengde(deltakelsesprosent = 50F, dagerPerUke = 2F, gyldigFra = LocalDate.now(), null),
            DeltakerEndring.Endring.EndreStartdato(startdato = LocalDate.now(), sluttdato = LocalDate.now().plusWeeks(2), null),
            DeltakerEndring.Endring.EndreSluttdato(sluttdato = LocalDate.now(), null),
            DeltakerEndring.Endring.EndreSluttarsak(
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
                begrunnelse = null,
            ),
            DeltakerEndring.Endring.ForlengDeltakelse(LocalDate.now(), "begrunnelse"),
            DeltakerEndring.Endring.IkkeAktuell(
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
                begrunnelse = "begrunnelse",
            ),
            DeltakerEndring.Endring.AvsluttDeltakelse(
                sluttdato = LocalDate.now(),
                aarsak = DeltakerEndring.Aarsak(
                    DeltakerEndring.Aarsak.Type.ANNET,
                    "beskrivelse",
                ),
                begrunnelse = "begrunnelse",
            ),
            DeltakerEndring.Endring.ReaktiverDeltakelse(
                reaktivertDato = LocalDate.now(),
                begrunnelse = "begrunnelse",
            ),
            DeltakerEndring.Endring.FjernOppstartsdato(
                begrunnelse = "begrunnelse",
            ),
        )

        endringer.forEach { endring ->
            MockResponseHandler.addEndringsresponse(
                deltaker.endre(TestData.lagDeltakerEndring(deltakerId = deltaker.id, endring = endring)),
                endring,
            )

            val oppdatertDeltaker = service.oppdaterDeltaker(
                deltaker,
                endring,
                "navIdent",
                "enhetsnummer",
            )

            when (endring) {
                is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> {
                    oppdatertDeltaker.bakgrunnsinformasjon shouldBe endring.bakgrunnsinformasjon
                }

                is DeltakerEndring.Endring.EndreInnhold -> {
                    oppdatertDeltaker.deltakelsesinnhold!!.innhold shouldBe endring.innhold
                    oppdatertDeltaker.deltakelsesinnhold!!.ledetekst shouldBe endring.ledetekst
                }

                is DeltakerEndring.Endring.EndreDeltakelsesmengde -> {
                    oppdatertDeltaker.deltakelsesprosent shouldBe endring.deltakelsesprosent
                    oppdatertDeltaker.dagerPerUke shouldBe endring.dagerPerUke
                }

                is DeltakerEndring.Endring.EndreStartdato -> {
                    oppdatertDeltaker.startdato shouldBe endring.startdato
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.EndreSluttdato -> {
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.EndreSluttarsak -> {
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                }

                is DeltakerEndring.Endring.ForlengDeltakelse -> {
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.IkkeAktuell -> {
                    oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.IKKE_AKTUELL
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                }

                is DeltakerEndring.Endring.AvsluttDeltakelse -> {
                    oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
                    oppdatertDeltaker.status.aarsak shouldBe endring.aarsak.toDeltakerStatusAarsak()
                    oppdatertDeltaker.sluttdato shouldBe endring.sluttdato
                }

                is DeltakerEndring.Endring.ReaktiverDeltakelse -> {
                    oppdatertDeltaker.status.type shouldBe DeltakerStatus.Type.VENTER_PA_OPPSTART
                    oppdatertDeltaker.startdato shouldBe null
                    oppdatertDeltaker.sluttdato shouldBe null
                }

                is DeltakerEndring.Endring.FjernOppstartsdato -> {
                    oppdatertDeltaker.startdato shouldBe null
                    oppdatertDeltaker.sluttdato shouldBe null
                }
            }
        }
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har ikke andre deltakelser - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            deltakelsesinnhold = null,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har ikke andre deltakelser, har sluttet - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.HAR_SLUTTET),
        )
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = LocalDate.now().minusMonths(2),
            sluttdato = LocalDate.now().minusDays(2),
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            deltakelsesinnhold = Deltakelsesinnhold("ny ledetekst", listOf(Innhold("", "", true, ""))),
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.HAR_SLUTTET,
                aarsak = DeltakerStatus.Aarsak.Type.ANNET,
                beskrivelse = "Oppdatert",
            ),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.HAR_SLUTTET
        deltakerFraDb.status.aarsak?.type shouldBe DeltakerStatus.Aarsak.Type.ANNET
        deltakerFraDb.status.aarsak?.beskrivelse shouldBe "Oppdatert"
        deltakerFraDb.deltakelsesinnhold!!.innhold shouldBe deltakeroppdatering.deltakelsesinnhold!!.innhold
        deltakerFraDb.deltakelsesinnhold!!.ledetekst shouldBe deltakeroppdatering.deltakelsesinnhold!!.ledetekst
        deltakerFraDb.kanEndres shouldBe true
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har tidligere deltakelse, statusoppdatering - setter kan ikke endres`() {
        val deltaker = TestData.lagDeltakerKladd()
        TestRepository.insert(deltaker)
        val gammelDeltaker = TestData.lagDeltaker(
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            kanEndres = true,
        )
        TestRepository.insert(gammelDeltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            deltakelsesinnhold = Deltakelsesinnhold("ny ledetekst", listOf(Innhold("", "", true, ""))),
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.UTKAST_TIL_PAMELDING),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.UTKAST_TIL_PAMELDING

        val gammelDeltakerFraDb = service.get(gammelDeltaker.id).getOrThrow()
        gammelDeltakerFraDb.kanEndres shouldBe false
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - har tidligere deltakelse, ikke statusoppdatering - oppdaterer deltaker`() {
        val deltaker = TestData.lagDeltaker(
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            startdato = LocalDate.now().minusDays(10),
            sluttdato = null,
        )
        TestRepository.insert(deltaker)
        val gammelDeltaker = TestData.lagDeltaker(
            deltakerliste = deltaker.deltakerliste,
            navBruker = deltaker.navBruker,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.IKKE_AKTUELL),
            kanEndres = true,
        )
        TestRepository.insert(gammelDeltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = LocalDate.now().minusDays(10),
            sluttdato = LocalDate.now().plusDays(10),
            dagerPerUke = null,
            deltakelsesprosent = 100F,
            bakgrunnsinformasjon = "Tekst",
            deltakelsesinnhold = null,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.DELTAR),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.DELTAR

        val gammelDeltakerFraDb = service.get(gammelDeltaker.id).getOrThrow()
        gammelDeltakerFraDb.kanEndres shouldBe true
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - feilregistrert - setter kan ikke endres`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            bakgrunnsinformasjon = null,
            deltakelsesinnhold = null,
            status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.FEILREGISTRERT),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.FEILREGISTRERT
        deltakerFraDb.kanEndres shouldBe false
    }

    @Test
    fun `oppdaterDeltaker(deltakerOppdatering) - avlyst gjennomforing - setter kan ikke endres`() {
        val deltaker = TestData.lagDeltaker(status = TestData.lagDeltakerStatus(type = DeltakerStatus.Type.VENTER_PA_OPPSTART))
        TestRepository.insert(deltaker)
        val deltakeroppdatering = Deltakeroppdatering(
            id = deltaker.id,
            startdato = null,
            sluttdato = null,
            dagerPerUke = null,
            deltakelsesprosent = null,
            bakgrunnsinformasjon = null,
            deltakelsesinnhold = null,
            status = TestData.lagDeltakerStatus(
                type = DeltakerStatus.Type.IKKE_AKTUELL,
                aarsak = DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT,
            ),
            historikk = emptyList(),
        )

        service.oppdaterDeltaker(deltakeroppdatering)

        val deltakerFraDb = service.get(deltaker.id).getOrThrow()
        deltakerFraDb.status.type shouldBe DeltakerStatus.Type.IKKE_AKTUELL
        deltakerFraDb.status.aarsak?.type shouldBe DeltakerStatus.Aarsak.Type.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT
        deltakerFraDb.kanEndres shouldBe false
    }
}

fun DeltakerEndring.Aarsak.toDeltakerStatusAarsak() = no.nav.amt.lib.models.deltaker.DeltakerStatus.Aarsak(
    no.nav.amt.lib.models.deltaker.DeltakerStatus.Aarsak.Type
        .valueOf(type.name),
    beskrivelse,
)
