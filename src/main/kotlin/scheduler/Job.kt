package scheduler

interface Job<ID> {
    val name: String
    val id: ID

    suspend fun run()
}

interface SupervisedJob<ID> : Job<ID> {
    val job: kotlinx.coroutines.Job
}

fun <ID> Job<ID>.job(job: kotlinx.coroutines.Job): SupervisedJob<ID> = object : SupervisedJob<ID>, Job<ID> by this {
    override val job: kotlinx.coroutines.Job = job
}