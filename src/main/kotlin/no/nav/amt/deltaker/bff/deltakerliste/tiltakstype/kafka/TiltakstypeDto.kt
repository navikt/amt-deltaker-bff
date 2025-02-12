package no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.kafka

import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.deltaker.bff.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import java.util.UUID

data class TiltakstypeDto(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakstype.Tiltakskode,
    val arenaKode: String?,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnhold?,
) {
    fun toModel(arenaKode: String): Tiltakstype {
        return Tiltakstype(
            id = id,
            navn = navn,
            tiltakskode = tiltakskode,
            arenaKode = Tiltakstype.ArenaKode.valueOf(arenaKode),
            innsatsgrupper = innsatsgrupper,
            innhold = deltakerRegistreringInnhold,
        )
    }
}
