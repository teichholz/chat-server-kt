package scheduler

import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.resilience.Schedule
import arrow.resilience.retry
import chat.commons.protocol.MessagePayloadSocket
import chat.commons.protocol.Protocol
import chat.commons.routing.ReceiverPayload
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
            catch({
                transaction {
                    messageRepository.sortedMessagesWithOffset(id, receiverSession.lastMessage).onEach {
                        val message =
                            either { it.domain() }.getOrNull() ?: throw IllegalStateException("SHOULD NOT HAPPEN")
                        var payload: MessagePayloadSocket?

                        receiverSession.session {
                            payload = MessagePayloadSocket(
                                ReceiverPayload(message.receiver.name),
                                ReceiverPayload(message.sender.name),
                                message.content,
                                message.date
                            )
                            sendSerialized(payload)
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
                    }.catch {
                        logger.error("Error trying to send messages: $it")
                    }.retry(Schedule.recurs(3)).launchIn(CoroutineScope(Dispatchers.IO))
                }
            }) {
                logger.error("Error trying to send messages: $it");
            }
        }
    }
}