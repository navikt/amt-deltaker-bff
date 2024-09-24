package no.nav.amt.deltaker.bff.deltaker

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.deltakerliste.DeltakerlisteRepository
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeltakerV2ConsumerTest {
    private val deltakerService = mockk<DeltakerService>(relaxUnitFun = true)
    private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()

    @Before
    fun setup() {
        every { deltakerlisteRepository.get(any()) } returns Result.success(
            TestData.lagDeltakerliste(
                tiltak = TestData.lagTiltakstype(tiltakskode = Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            ),
        )
    }

    @Test
    fun `consume - kilde er ARENA - konsumerer melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService, deltakerlisteRepository)
        val deltaker = TestData.lagDeltaker()

        consumer.consume(
            deltaker.id,
            objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
        )

        verify(exactly = 1) { deltakerService.oppdaterDeltaker(any()) }
    }

    @Test
    fun `consume - kilde er KOMET - konsumerer melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService, deltakerlisteRepository)
        val deltaker = TestData.lagDeltaker()

        consumer.consume(
            deltaker.id,
            objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.KOMET)),
        )
        verify(exactly = 1) { deltakerService.oppdaterDeltaker(any()) }
    }

    @Test
    fun `consume - tombstone - konsumerer ikke melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService, deltakerlisteRepository)
        consumer.consume(UUID.randomUUID(), null)
        verify(exactly = 0) { deltakerService.oppdaterDeltaker(any()) }
    }
}

private fun Deltaker.toV2(kilde: DeltakerV2Dto.Kilde) = DeltakerV2Dto(
    id = id,
    deltakerlisteId = deltakerliste.id,
    status = DeltakerV2Dto.DeltakerStatusDto(
        id = status.id,
        type = status.type,
        aarsak = status.aarsak?.type,
        aarsaksbeskrivelse = status.aarsak?.beskrivelse,
        gyldigFra = status.gyldigFra,
        opprettetDato = status.opprettet,
    ),
    dagerPerUke = dagerPerUke,
    prosentStilling = deltakelsesprosent?.toDouble(),
    oppstartsdato = startdato,
    sluttdato = sluttdato,
    bestillingTekst = bakgrunnsinformasjon,
    kilde = kilde,
    innhold = deltakelsesinnhold,
    historikk = historikk,
)
