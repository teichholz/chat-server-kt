package scheduler

import arrow.fx.coroutines.ResourceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logger.LoggerDelegate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration

object Scheduler : KoinComponent {
    private val logger by LoggerDelegate()
    private val parentSupervisor = SupervisorJob()

    private val jobs by inject<JobRegistry<Int>>()

    context(ResourceScope)
    suspend fun install() {
        install({ parentSupervisor }) { jobSupervisor, _ ->
            jobSupervisor.cancelAndJoin()
            logger.info("Stoppe scheduler and scheduled jobs")
        }
    }

    fun schedule(period: Duration, job: Job<Int>) {
        jobs += job.supervise(parentSupervisor)

        logger.info("Scheduling job ${job.name}")
        tickerFlow(period).onEach {
            job.run()
            //logger.info("Job step ${it} ($period) for ${job.name} finished")
        }.launchIn(CoroutineScope(parentSupervisor + Dispatchers.IO))
    }

    private fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
        var counter = 0L
        delay(initialDelay)
        while (true) {
            emit(++counter)
            delay(period)
        }
    }
}

