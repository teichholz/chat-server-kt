package scheduler

interface Job<ID> {
    val name: String
    val id: ID

    suspend fun run()
}

interface SupervisedJob<ID> : Job<ID> {
    val supervisor: kotlinx.coroutines.Job
}

fun <ID> Job<ID>.supervise(supervisor: kotlinx.coroutines.Job): SupervisedJob<ID> = object : SupervisedJob<ID>, Job<ID> by this {
    override val supervisor: kotlinx.coroutines.Job = supervisor
}