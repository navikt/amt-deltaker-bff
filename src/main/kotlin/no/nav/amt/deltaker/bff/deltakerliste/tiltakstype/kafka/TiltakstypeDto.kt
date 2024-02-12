package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka

import no.nav.amt.deltaker.bff.deltakerliste.Tiltak
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import java.util.UUID

data class TiltakstypeDto(
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    val status: Tiltakstypestatus,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnhold?,
) {
    fun toModel(): Tiltakstype {
        return Tiltakstype(
            id = id,
            navn = navn,
            type = Tiltak.Type.valueOf(arenaKode),
            innhold = deltakerRegistreringInnhold,
        )
    }
}

enum class Tiltakstypestatus {
    Aktiv,
    Planlagt,
    Avsluttet,
}
