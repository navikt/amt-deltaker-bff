package no.nav.amt.deltaker.bff.kafka.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.Environment
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorDeltakerlisteTilgang
import no.nav.amt.deltaker.bff.auth.TiltakskoordinatorsDeltakerlisteDto
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.shouldBeCloseTo
import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

fun assertProduced(forslag: Forslag) {
    val cache = mutableMapOf<UUID, Forslag>()

    val consumer = stringStringConsumer(Environment.ARRANGOR_MELDING_TOPIC) { k, v ->
        cache[UUID.fromString(k)] = objectMapper.readValue(v!!)
    }

    consumer.start()

    AsyncUtils.eventually {
        val cachedForslag = cache[forslag.id]!!
        cachedForslag.id shouldBe forslag.id
        cachedForslag.deltakerId shouldBe forslag.deltakerId
        cachedForslag.endring shouldBe forslag.endring
        cachedForslag.begrunnelse shouldBe forslag.begrunnelse
        cachedForslag.opprettet shouldBe forslag.opprettet
        cachedForslag.opprettetAvArrangorAnsattId shouldBe forslag.opprettetAvArrangorAnsattId
        sammenlignForslagStatus(cachedForslag.status, forslag.status)
    }

    consumer.stop()
}

fun assertProduced(tilgang: TiltakskoordinatorsDeltakerlisteDto, tombstoneExpected: Boolean = false) {
    val cache = mutableMapOf<UUID, TiltakskoordinatorsDeltakerlisteDto?>()

    val consumer = stringStringConsumer(Environment.AMT_TILTAKSKOORDINATORS_DELTAKERLISTE_TOPIC) { k, v ->
        cache[UUID.fromString(k)] = v?.let { objectMapper.readValue(it) }
    }

    consumer.start()

    AsyncUtils.eventually {
        cache.keys.contains(tilgang.id) shouldBe true

        if (tombstoneExpected) {
            cache[tilgang.id] shouldBe null
        } else {
            assertSoftly(cache[tilgang.id].shouldNotBeNull()) {
                id shouldBe tilgang.id
                gjennomforingId shouldBe tilgang.gjennomforingId
                navIdent shouldBe tilgang.navIdent
            }
        }
    }

    consumer.stop()
}

fun assertProducedTombstone(tilgang: TiltakskoordinatorDeltakerlisteTilgang) {
    val cache = mutableMapOf<UUID, TiltakskoordinatorsDeltakerlisteDto?>()

    val consumer = stringStringConsumer(Environment.AMT_TILTAKSKOORDINATORS_DELTAKERLISTE_TOPIC) { k, v ->
        cache[UUID.fromString(k)] = v?.let { objectMapper.readValue(it) }
    }

    consumer.start()

    AsyncUtils.eventually {
        cache.keys.contains(tilgang.id) shouldBe true
        cache[tilgang.id] shouldBe null
    }

    consumer.stop()
}

fun sammenlignForslagStatus(a: Forslag.Status, b: Forslag.Status) {
    when (a) {
        is Forslag.Status.VenterPaSvar -> {
            b as Forslag.Status.VenterPaSvar
            a shouldBe b
        }

        is Forslag.Status.Avvist -> {
            b as Forslag.Status.Avvist
            a.avvist shouldBeCloseTo b.avvist
            a.avvistAv shouldBe b.avvistAv
            a.begrunnelseFraNav shouldBe b.begrunnelseFraNav
        }

        is Forslag.Status.Godkjent -> {
            b as Forslag.Status.Godkjent
            a.godkjent shouldBeCloseTo b.godkjent
            a.godkjentAv shouldBe b.godkjentAv
        }

        is Forslag.Status.Tilbakekalt -> {
            b as Forslag.Status.Tilbakekalt
            a.tilbakekalt shouldBeCloseTo b.tilbakekalt
            a.tilbakekaltAvArrangorAnsattId shouldBe b.tilbakekaltAvArrangorAnsattId
        }

        is Forslag.Status.Erstattet -> {
            b as Forslag.Status.Erstattet
            a.erstattetMedForslagId shouldBe b.erstattetMedForslagId
            a.erstattet shouldBeCloseTo b.erstattet
        }
    }
}
