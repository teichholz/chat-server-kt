import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.awaitCancellation
import java.util.*
import kotlin.time.Duration.Companion.seconds

class JobTest : FunSpec() {
    init {
        test("timer") {
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                }
            }

            Timer().scheduleAtFixedRate(task, Date(), 10.seconds.inWholeMilliseconds)

            awaitCancellation()
        }
    }
}