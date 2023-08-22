import arrow.core.raise.either
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.MessageRepository
import repository.domain
import repository.transaction
import routing.MessagePayload
import routing.ReceiverPayload
import java.util.*

context(WebSocketServerSession)
class DeliverMessagesJob(val receiverId: Int) : TimerTask(), KoinComponent {

    val messageRepository: MessageRepository by inject()

    override fun run() {
        runBlocking {
            val unsent = transaction {
                messageRepository.unsent(receiverId)
            }

            unsent.collect {
                val record = it
                transaction {
                    either {
                        it.domain()
                    }.onRight {
                        sendSerialized(MessagePayload(ReceiverPayload(it.sender.name), it.content, receiverId))
                        record.sent = true
                        messageRepository.save(record)
                    }
                }
            }
        }
    }
}