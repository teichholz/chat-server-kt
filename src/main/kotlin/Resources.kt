import arrow.fx.coroutines.ResourceScope
import io.ktor.network.selector.*
import io.ktor.network.sockets.*

suspend fun ResourceScope.selectorManager(): SelectorManager =
    install({ SelectorManager(kotlinx.coroutines.Dispatchers.IO) }) { selector, _ ->
        selector.close().also { println("Selector closed") }
    }

suspend fun ResourceScope.serverSocket(): ServerSocket {
    val selectorManager = selectorManager()
    return install({ aSocket(selectorManager).tcp().bind("127.0.0.1", 9002) }) { socket, _ -> socket.close() }
}

suspend fun ResourceScope.socket(): Socket = socket(selectorManager())

suspend fun ResourceScope.socket(selectorManager: SelectorManager): Socket =
    install({
        aSocket(selectorManager).tcp().connect("127.0.0.1", 9002)
    }) { socket, _ -> socket.close().also { println("socket closed") } }


