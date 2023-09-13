package scheduler

import arrow.core.raise.either
import chat.commons.protocol.MessagePayloadSocket
import chat.commons.protocol.Protocol
import chat.commons.protocol.message
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
        connections.entries.forEach { (id, receiverSession) ->
            transaction {
                messageRepository.sortedMessagesWithOffset(id, receiverSession.lastMessage)
            }.forEach {
                val msg = transaction {
                    either { it.domain() }.getOrNull() ?: throw IllegalStateException("SHOULD NOT HAPPEN")
                }
                receiverSession.session {
                    sendSerialized(message {
                        this.payload = MessagePayloadSocket(
                            ReceiverPayload(msg.sender.name),
                            ReceiverPayload(msg.receiver.name),
                            msg.content,
                            msg.date
                        )
                    })

                    val protocol: Protocol = receiveDeserialized()
                    when (protocol) {
                        is Protocol.ACK -> {
                            if (protocol.payload != receiverSession.lastMessage + 1) {
                                throw IllegalStateException("ACK not in order")
                            }
                        }

                        else -> throw IllegalStateException("SHOULD NOT HAPPEN")
                    }
                    connections.incrementMessageCount(id)
                }
            }
        }
    }

}