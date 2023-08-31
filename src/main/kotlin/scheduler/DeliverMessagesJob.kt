package scheduler

import arrow.core.raise.catch
import arrow.core.raise.either
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import logger.LoggerDelegate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import protocol.ACK
import repository.MessageRepository
import repository.domain
import repository.transaction
import routing.MessagePayloadJob
import routing.ReceiverPayload

object DeliverMessagesJob : Job<Int>, KoinComponent {
    val logger: Logger by LoggerDelegate()

    val messageRepository: MessageRepository by inject()
    val connections: Connections by inject()

    override val name: String
        get() = javaClass.simpleName
    override val id
        get() = 42

    override suspend fun run() {
        connections.entries.forEach { (id, session) ->
            catch({
                transaction {
                    messageRepository.messagesAfterTimestamp(id, session.timestamp).collect {
                        val message =
                            either { it.domain() }.getOrNull() ?: throw IllegalStateException("SHOULD NOT HAPPEN")
                        var payload: MessagePayloadJob?

                        session.session {
                            payload = MessagePayloadJob(
                                ReceiverPayload(message.receiver.name),
                                message.content,
                                message.date
                            )
                            sendSerialized(payload)
                            val ack: ACK = receiveDeserialized()
                            connections.updateTimestamp(id, message.date)
                        }
                    }
                }
            }) {
                when (it) {
                    is ClosedSendChannelException -> {
                        connections -= id
                    }
                    is ClosedReceiveChannelException -> {
                        connections -= id
                    }
                }
                logger.error("Error trying to send messages: $it");
            }
        }
    }
}