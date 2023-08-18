import arrow.continuations.SuspendApp
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * TODO the moment a client connects, the server has to send unsent messages
 */

fun main(args: Array<String>) = SuspendApp {
    either {
        resourceScope {
            val socket = socket()

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            launch(Dispatchers.IO) {
                while (isActive) {
                    val greeting = receiveChannel.readUTF8Line()

                    if (greeting != null) {
                        println(greeting)
                    } else {
                        raise("Server closed connection")
                    }
                }
            }

            while (isActive) {
                val myMessage = readln()
                sendChannel.writeStringUtf8("$myMessage\n")
            }
        }
    }.onLeft {
        when (it) {
            is String -> println(it)
        }
    }
}

