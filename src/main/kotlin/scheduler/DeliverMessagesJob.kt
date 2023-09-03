package scheduler

import arrow.core.raise.catch
import arrow.core.raise.either
import chat.commons.protocol.MessagePayloadSocket
import chat.commons.protocol.Protocol
import chat.commons.routing.ReceiverPayload
import io.ktor.server.websocket.*
import logger.LoggerDelegate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import repository.MessageRepository
import repository.domain
import repository.transaction

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
                    messageRepository.sortedMessagesWithOffset(id, session.lastMessage).collect {
                        val message =
                            either { it.domain() }.getOrNull() ?: throw IllegalStateException("SHOULD NOT HAPPEN")
                        var payload: MessagePayloadSocket?

                        session.session {
                            payload = MessagePayloadSocket(
                                ReceiverPayload(message.receiver.name),
                                message.content,
                                message.date
                            )
                            sendSerialized(payload)
                            val ack: Protocol<*> = receiveDeserialized()
                            when (ack.type) {
                                "ACK" -> {}
                                else -> throw IllegalStateException("SHOULD NOT HAPPEN")
                            }
                            connections.incrementMessageCount(id)
                        }
                    }
                }
            }) {
                connections -= id
                logger.error("Error trying to send messages: $it");
            }
        }
    }
}