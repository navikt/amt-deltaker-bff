package no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse

import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.extensions.toUlestHendelse
import no.nav.amt.deltaker.bff.tiltakskoordinator.ulesthendelse.model.UlestHendelse
import no.nav.amt.lib.models.hendelse.Hendelse
import org.slf4j.LoggerFactory
import java.util.UUID

class UlestHendelseService(
    private val ulestHendelseRepository: UlestHendelseRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun delete(id: UUID) {
        ulestHendelseRepository.delete(id)
        log.info("Slettet ulest hendelse $id")
    }

    fun lagreUlestHendelse(hendelse: Hendelse) {
        val ulestHendelse = hendelse.toUlestHendelse()
        if (ulestHendelse != null) {
            ulestHendelseRepository.upsert(ulestHendelse)
            log.info("Lagret ulest hendelse ${hendelse.id} for deltaker ${hendelse.deltaker.id}")
        } else {
            log.warn("Ikke lagret ulest hendelse ${hendelse.id} for deltaker ${hendelse.deltaker.id}")
        }
    }

    fun getUlessteHendelserForDeltaker(deltaker_id: UUID): List<UlestHendelse> = ulestHendelseRepository.getForDeltaker(deltaker_id)

    fun getUlessteHendelserForDeltakere(deltaker_ider: List<UUID>): List<UlestHendelse> =
        ulestHendelseRepository.getForDeltakere(deltaker_ider)
}
