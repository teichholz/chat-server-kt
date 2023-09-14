package scheduler

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import logger.LoggerDelegate
import model.Receiver
import org.koin.ktor.ext.inject
import java.util.*

data class ReceiverSession(val receiver: Receiver, val lastMessage: Long, val receive: WebSocketServerSession? = null, val send: WebSocketServerSession? = null) {}

/**
 * This class is responsible for keeping track of the websocket connections to the current server
 */
class Connections(private val users: MutableMap<Int, ReceiverSession> = Collections.synchronizedMap(mutableMapOf())) : MutableMap<Int, ReceiverSession> by users {
    val logger by LoggerDelegate()

    context(WebSocketServerSession)
    fun messageCount(): Long {
        val principal = call.principal<Receiver>()!!
        return users[principal.id]?.lastMessage ?: 0
    }

    fun incrementMessageCount(receiverId: Int): Long {
        users[receiverId]?.let {
            users[receiverId] = it.copy(lastMessage = it.lastMessage + 1)
        }
        return users[receiverId]?.lastMessage ?: 0
    }

    context(WebSocketServerSession)
    fun incrementMessageCount(): Long {
        val principal = call.principal<Receiver>()!!
        return incrementMessageCount(principal.id)
    }
}

context(Route)
fun WebSocketServerSession.currentSession(): ReceiverSession {
    val connections by inject<Connections>()
    return connections[call.principal<Receiver>()!!.id]!!
}