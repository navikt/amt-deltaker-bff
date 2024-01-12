package no.nav.amt.deltaker.bff.utils

import io.kotest.matchers.shouldBe
import no.nav.amt.deltaker.bff.endringsmelding.Endringsmelding

infix fun Endringsmelding?.shouldBe(other: Endringsmelding?) {
    if (this == null) {
        other shouldBe null
    } else {
        this.id shouldBe other?.id
        this.deltakerId shouldBe other?.deltakerId
        this.opprettetAvArrangorAnsattId shouldBe other?.opprettetAvArrangorAnsattId
        this.createdAt shouldBeCloseTo other?.createdAt
        this.utfortAvNavAnsattId shouldBe other?.utfortAvNavAnsattId
        this.utfortTidspunkt shouldBeCloseTo other?.utfortTidspunkt
        this.status shouldBe other?.status
        this.type shouldBe other?.type
        this.innhold shouldBe other?.innhold
    }
}
