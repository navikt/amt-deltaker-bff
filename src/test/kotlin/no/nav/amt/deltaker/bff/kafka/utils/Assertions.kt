package no.nav.amt.deltaker.bff.kafka.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.application.plugins.objectMapper
import no.nav.amt.deltaker.bff.deltaker.kafka.DeltakerDto
import no.nav.amt.deltaker.bff.deltaker.kafka.toDto
import no.nav.amt.deltaker.bff.deltaker.model.Deltaker
import no.nav.amt.deltaker.bff.utils.AsyncUtils
import no.nav.amt.deltaker.bff.utils.shouldBeCloseTo
import java.util.UUID

fun assertProduced(deltaker: Deltaker) {
    val cache = mutableMapOf<UUID, DeltakerDto>()

    val consumer = stringStringConsumer(Environment.DELTAKER_ENDRING_TOPIC) { k, v ->
        cache[UUID.fromString(k)] = objectMapper.readValue(v)
    }

    consumer.run()

    AsyncUtils.eventually {
        val cachedDeltaker = cache[deltaker.id]!!
        cachedDeltaker.id shouldBe deltaker.id
        cachedDeltaker.personId shouldBe deltaker.navBruker.personId
        cachedDeltaker.personident shouldBe deltaker.navBruker.personident
        cachedDeltaker.deltakerlisteId shouldBe deltaker.deltakerliste.id
        cachedDeltaker.startdato shouldBe deltaker.startdato
        cachedDeltaker.sluttdato shouldBe deltaker.sluttdato
        cachedDeltaker.dagerPerUke shouldBe deltaker.dagerPerUke
        cachedDeltaker.deltakelsesprosent shouldBe deltaker.deltakelsesprosent
        cachedDeltaker.bakgrunnsinformasjon shouldBe deltaker.bakgrunnsinformasjon
        cachedDeltaker.innhold?.ledetekst shouldBe deltaker.deltakerliste.tiltak.innhold?.ledetekst
        cachedDeltaker.innhold?.innhold shouldBe deltaker.innhold.toDto()
        cachedDeltaker.status.type shouldBe deltaker.status.type
        cachedDeltaker.status.aarsak shouldBe deltaker.status.aarsak
        cachedDeltaker.status.gyldigTil shouldBeCloseTo deltaker.status.gyldigTil
        cachedDeltaker.status.gyldigFra shouldBeCloseTo deltaker.status.gyldigFra
        cachedDeltaker.status.opprettet shouldBeCloseTo deltaker.status.opprettet
        cachedDeltaker.sistEndret shouldBeCloseTo deltaker.sistEndret
        cachedDeltaker.opprettet shouldBeCloseTo deltaker.opprettet
    }

    consumer.stop()
}
