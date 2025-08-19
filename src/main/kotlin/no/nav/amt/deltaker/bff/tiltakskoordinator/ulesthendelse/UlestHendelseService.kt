package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse

import no.nav.amt.lib.models.hendelse.Hendelse
import org.slf4j.LoggerFactory

class UlestHendelseService(
    private val ulestHendelseRepository: UlestHendelseRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun delete(id: java.util.UUID) {
        ulestHendelseRepository.delete(id)
        log.info("Slettet ulest hendelse $id")
    }

    fun lagreUlestHendelse(hendelse: Hendelse) {
        ulestHendelseRepository.upsert(hendelse)
        log.info("Lagret ulest hendelse ${hendelse.id} for deltaker ${hendelse.deltaker.id}")
    }
}
