package scheduler

import arrow.fx.coroutines.ResourceScope
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.datetime.LocalDateTime
import model.Receiver
import java.util.*

data class ReceiverSession(val receiver: Receiver, val timestamp: LocalDateTime?, val session: WebSocketServerSession) {
    suspend fun session(block: suspend WebSocketServerSession.() -> Unit) = session.block()
}

class Connections(private val users: MutableMap<Int, ReceiverSession> = Collections.synchronizedMap(mutableMapOf())) : MutableMap<Int, ReceiverSession> by users {
    fun updateTimestamp(receiverId: Int, timestamp: LocalDateTime) {
        users[receiverId]?.let {
            users[receiverId] = it.copy(timestamp = timestamp)
        }
    }

    context(ResourceScope)
    suspend fun install() {
        install({ this@Connections }) { connections, _ ->
            connections.values.forEach {
                it.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shut down"))
                connections -= it.receiver.id
            }
        }
    }
}