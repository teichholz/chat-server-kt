import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import logger.getLogger
import model.tables.records.ReceiverRecord
import org.koin.ksp.generated.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level
import repository.MessageRepository
import repository.ReceiverRepository
import repository.RepositoryModule
import repository.transaction
import routing.MessagePayload
import routing.ReceiverPayload
import routing.ReceiverPayloadWithId

val logger = getLogger("Application")

fun main() = SuspendApp {
    resourceScope {
        server(Netty, host = "127.0.0.1", port = 8080) {
            di()
            logging()
            webSockets()
            routing()
        }


        awaitCancellation()
    }
}

fun Application.di() {
    install(Koin) {
        slf4jLogger()
        modules(RepositoryModule().module)
        logger.info("Koin installed")
    }
}

fun Application.logging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    webSockets()
}

fun Application.webSockets() {
    install(WebSockets) {
        // ...
    }
}

fun Application.routing() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    install(RequestValidation)
    routing {
        val messageRepository: MessageRepository by inject()
        val receiverRepository: ReceiverRepository by inject()

        post("/user/register") {
            val receiver = call.receive<ReceiverPayload>()
            val saved = transaction { receiverRepository.save(ReceiverRecord(null, receiver.name)) }
            call.respond(saved.id)
        }

        post("/user/logout") {
            val id = call.receive<Int>()
            either {
                transaction { receiverRepository.delete(id) }
            }
        }

        post("/user/configure") {
            val receiverPayload = call.receive<ReceiverPayloadWithId>()
            either {
                transaction {
                    val receiver = receiverRepository.load(receiverPayload.id)
                    receiver.name = receiverPayload.name
                    receiver.store()
                }
            }.onLeft {
                call.respond("User not found");
            }.onRight {
                call.respond("Registered user")
            }

        }

        post("/send") {
            val message = call.receive<MessagePayload>()
            either {
                val receiver = transaction { receiverRepository.load(message.receiver) }
            }
        }

        webSocket("/chat") {
            send("You are connected!")
            for(frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                send("You said: $receivedText")
            }
        }
    }
}