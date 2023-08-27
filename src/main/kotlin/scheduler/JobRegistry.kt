package scheduler

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.cancelAndJoin

sealed class JobException(override val message: String) : Throwable(message) {
    data class JobNotFound(val id: Any) : JobException("Job with id $id not found") {}
}

class JobRegistry<ID : Any> {
    val jobs: MutableMap<ID, SupervisedJob<ID>> = mutableMapOf()

    fun register(job: SupervisedJob<ID>) {
        jobs += job.id to job
    }

    operator fun plusAssign(job: SupervisedJob<ID>) = register(job)

    context(Raise<JobException.JobNotFound>)
    suspend fun unregister(id: ID) {
        val job = jobs[id]
        ensureNotNull(job) { JobException.JobNotFound(id) }

        job.supervisor.cancelAndJoin()
        jobs -= id
    }

    operator fun get(key: ID): Job<ID>? = jobs[key]
}