package scheduler

import arrow.core.Either
import arrow.core.raise.either
import arrow.resilience.Schedule
import arrow.resilience.retry
import io.ktor.server.websocket.*
import logger.LoggerDelegate
import model.Receiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import repository.MessageRepository
import repository.domain
import repository.transaction
import routing.MessagePayloadJob
import routing.ReceiverPayload
import kotlin.time.Duration.Companion.seconds

context(WebSocketServerSession)
class DeliverMessagesJob(val receiver: Receiver) : Job<Int>, KoinComponent {
    val logger: Logger by LoggerDelegate()

    val messageRepository: MessageRepository by inject()

    override val name: String
        get() = "DeliverMessagesJob: $receiver.name"
    override val id
        get() = receiver.id

    override suspend fun run() {
        val unsent = transaction {
            messageRepository.unsent(receiver.id)
        }.retry(Schedule.recurs<Throwable>(5).zipLeft(Schedule.linear(10.seconds)))

        unsent.collect {
            val record = it
            transaction {
                var payload: MessagePayloadJob? = null
                Either.catch {
                    either {
                        val message = it.domain()
                        payload = MessagePayloadJob(
                            ReceiverPayload(message.receiver.name),
                            message.content,
                            message.date
                        )

                        sendSerialized(payload)
                        record.sent = true
                        messageRepository.save(record)
                    }
                }.onLeft {
                    logger.error("Error trying to send unsent messages", it)
                }.onRight {
                    logger.info("Sent unsent message: $payload")
                }
            }
        }
    }
}