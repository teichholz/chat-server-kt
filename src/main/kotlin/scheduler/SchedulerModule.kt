package scheduler

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
class SchedulerModule {

    @Single
    fun registry() = JobRegistry<Int>()
}