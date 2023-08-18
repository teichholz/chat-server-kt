import arrow.continuations.SuspendApp
import arrow.core.Either
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Main

fun main(args: Array<String>) = SuspendApp {
    resourceScope {
        val serverSocket = serverSocket()

        while (isActive) {
            val socket = serverSocket.accept()

            println("Accepted $socket")

            launchWithResources {
                install(socket)
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)


                sendChannel.writeStringUtf8("Please enter your name\n")

                while (isActive) {
                    val name = receiveChannel.readUTF8Line()
                    println("Received $name")
                    sendChannel.writeStringUtf8("Hello, $name!\nHow are you?\n")
                }
            }
        }
    }

    awaitCancellation()
}

fun CoroutineScope.launchWithResources(block: suspend ResourceScope.() -> Unit) {
    launch {
        resourceScope {
            Either.catch {
                block(this@resourceScope)
            }.onLeft {
                println("Error: ${it.message}")
            }
        }
    }
}

private suspend fun ResourceScope.install(socket: Socket) =
    install({ socket }) { socket, _ ->
        socket.close().also { println("Closing socket $socket") }
    }