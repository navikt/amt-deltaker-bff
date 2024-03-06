package no.nav.amt.deltaker.bff.deltaker

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.utils.data.TestData
import org.junit.Test
import java.util.UUID

class DeltakerV2ConsumerTest {

    private val deltakerService = mockk<DeltakerService>(relaxUnitFun = true)

    @Test
    fun `consume - kilde er ikke KOMET - konsumerer ikke melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService)
        val deltaker = TestData.lagDeltaker()

        consumer.consume(
            deltaker.id,
            objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.ARENA)),
        )

        verify(exactly = 0) { deltakerService.oppdaterDeltaker(any()) }
    }

    @Test
    fun `consume - kilde er KOMET - konsumerer melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService)
        val deltaker = TestData.lagDeltaker()

        consumer.consume(
            deltaker.id,
            objectMapper.writeValueAsString(deltaker.toV2(DeltakerV2Dto.Kilde.KOMET)),
        )
        verify(exactly = 1) { deltakerService.oppdaterDeltaker(any()) }
    }

    @Test
    fun `consume - tombstone - konsumerer ikke melding`() = runBlocking {
        val consumer = DeltakerV2Consumer(deltakerService)
        consumer.consume(UUID.randomUUID(), null)
        verify(exactly = 0) { deltakerService.oppdaterDeltaker(any()) }
    }
}

private fun Deltaker.toV2(kilde: DeltakerV2Dto.Kilde) = DeltakerV2Dto(
    id,
    status = DeltakerV2Dto.DeltakerStatusDto(
        id = status.id,
        type = status.type,
        aarsak = status.aarsak,
        gyldigFra = status.gyldigFra,
        opprettetDato = status.opprettet,
    ),
    dagerPerUke = dagerPerUke,
    prosentStilling = deltakelsesprosent?.toDouble(),
    oppstartsdato = startdato,
    sluttdato = sluttdato,
    bestillingTekst = bakgrunnsinformasjon,
    kilde = kilde,
    innhold = DeltakerV2Dto.Deltakelsesinnhold(
        deltakerliste.tiltak.innhold!!.ledetekst,
        innhold,
    ),
    historikk = historikk,
)
