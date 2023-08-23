import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import io.ktor.serialization.kotlinx.*
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
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
import repository.domain
import repository.transaction
import routing.MessagePayload
import routing.ReceiverPayloadLogin
import routing.ReceiverPayloadLogout
import routing.ReceiverPayloadRegister
import routing.RequestValidator
import routing.record
import scheduler.DeliverMessagesJob
import scheduler.Scheduler
import kotlin.time.Duration.Companion.minutes

val logger = getLogger("Application")

fun main() = SuspendApp {
    resourceScope {
        server(Netty, host = "127.0.0.1", port = 8080) {
            di()
            logging()
            webSockets()
            routing()
        }
        Scheduler.install()

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
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}

fun Application.routing() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    val messageRepository: MessageRepository by inject()
    val receiverRepository: ReceiverRepository by inject()

    install(RequestValidation) {
        val validator: RequestValidator by inject()
        validate<ReceiverPayloadRegister>(validator::validateNameNotTaken)
        validate<ReceiverPayloadLogin>(validator::validateSynch)
        validate<ReceiverPayloadLogout>(validator::validateSynch)
    }

    routing {
        webSocket("/user/register") {
            val receiver = receiveDeserialized<ReceiverPayloadRegister>()

            transaction {
                receiverRepository.save(ReceiverRecord(null, receiver.name)).domain()
            }.run {
                Scheduler.schedule(1.minutes, DeliverMessagesJob(this))
                call.respond(id)
            }
        }

        post("/user/logout") {
            val id = call.receive<ReceiverPayloadLogout>()
            val job = Scheduler.jobs[id.id]
            if (job != null) {
                job.supervisor.cancelAndJoin()
                Scheduler.jobs.remove(job.id)
            }
            call.respond("Logged out user $id")
        }

        post("/send") {
            val message = call.receive<MessagePayload>()
            either {
                transaction { receiverRepository.load(message.receiver) }
            }.onLeft {
                call.respond("Receiver not found")
            }.onRight {
                transaction {
                    messageRepository.save(message.record())
                }
                call.respond("Message will be sent")
            }
        }

        /*post("/user/configure") {
            val config: ReceiverPayloadConfiguration = call.receive()
            either {
                transaction {
                    val receiver = receiverRepository.load(config.from.id)
                    if (config.from.equals(receiver.payload())) {
                        // TODO: Name should be unique
                        receiver.name = config.to.name
                        receiverRepository.save(receiver)
                    } else {
                        raise("User mismatch")
                    }
                }
            }.onLeft {
                when (it) {
                    is String -> call.respond(it)
                    is SqlError.RecordNotFound -> call.respond("User Not Found")
                }
            }.onRight {
                call.respond("Configured user")
            }
        }*/

    }
}