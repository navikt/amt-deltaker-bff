package no.nav.amt.deltaker.bff.job

import io.ktor.util.Attributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.amt.deltaker.bff.application.isReadyKey
import no.nav.amt.deltaker.bff.job.leaderelection.LeaderElection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class StatusUpdateJob(
    private val leaderElection: LeaderElection,
    private val attributes: Attributes,
    private val deltakerStatusOppdateringService: DeltakerStatusOppdateringService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun startJob(): Timer {
        return fixedRateTimer(
            name = this.javaClass.simpleName,
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            period = Duration.of(1, ChronoUnit.HOURS).toMillis(),
        ) {
            scope.launch {
                if (leaderElection.isLeader() && attributes.getOrNull(isReadyKey) == true) {
                    try {
                        log.info("Kjører jobb for å oppdatere deltakerstatuser")
                        deltakerStatusOppdateringService.oppdaterDeltakerStatuser()
                        log.info("Ferdig med å oppdatere deltakerstatuser")
                    } catch (e: Exception) {
                        log.error("Noe gikk galt ved oppdatering av deltakerstatus", e)
                    }
                }
            }
        }
    }
}
