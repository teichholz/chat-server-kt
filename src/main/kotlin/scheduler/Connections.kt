package scheduler

import arrow.fx.coroutines.ResourceScope
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import logger.LoggerDelegate
import model.Receiver
import java.util.*

data class ReceiverSession(val receiver: Receiver, val lastMessage: Long, val session: WebSocketServerSession) {
    suspend fun session(block: suspend WebSocketServerSession.() -> Unit) = session.block()
}

class Connections(private val users: MutableMap<Int, ReceiverSession> = Collections.synchronizedMap(mutableMapOf())) : MutableMap<Int, ReceiverSession> by users {
    val logger by LoggerDelegate()

    fun updateLastMessage(receiverId: Int, lastMessage: Long) {
        users[receiverId]?.let {
            users[receiverId] = it.copy(lastMessage = lastMessage)
        }
    }

    fun incrementMessageCount(receiverId: Int) {
        users[receiverId]?.let {
            users[receiverId] = it.copy(lastMessage = it.lastMessage + 1)
        }
    }

    context(ResourceScope)
    suspend fun install() {
        install({ this@Connections }) { connections, _ ->
            connections.values.forEach {
                logger.info("Closing connection for ${it.receiver.name}")
                it.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shut down"))
                connections -= it.receiver.id
            }
        }
    }
}