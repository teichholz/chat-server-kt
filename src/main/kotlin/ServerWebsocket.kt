import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import di.di
import io.ktor.http.*
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
import io.ktor.websocket.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import logger.getLogger
import model.tables.records.ReceiverRecord
import org.koin.core.component.inject
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
import routing.RoutingModule
import routing.record
import scheduler.Connections
import scheduler.DeliverMessagesJob
import scheduler.JobRegistry
import scheduler.ReceiverSession
import scheduler.Scheduler
import scheduler.SchedulerModule
import kotlin.time.Duration.Companion.seconds

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
        di { inject<Connections>().value }.install()

        Scheduler.schedule(5.seconds, DeliverMessagesJob)

        awaitCancellation()
    }
}

fun Application.di() {
    install(Koin) {
        slf4jLogger()
        modules(RepositoryModule().module, RoutingModule().module, SchedulerModule().module)
        logger.info("Koin installed")
    }
}

fun Application.logging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
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
    val connections: Connections by inject()

    val jobs by inject<JobRegistry<Int>>()

    install(RequestValidation) {
        val validator: RequestValidator by inject()
        validate<ReceiverPayloadRegister>(validator::validateNameNotTaken)
        //validate<ReceiverPayloadLogin>(validator::validateSynch)
        validate<ReceiverPayloadLogout>(validator::validateSynch)
    }

    routing {
        webSocket("/user/register") {
            val receiver = receiveDeserialized<ReceiverPayloadRegister>()

            transaction {
                val record = receiverRepository.save(ReceiverRecord(null, receiver.name)).domain()
                connections += record.id to ReceiverSession(record, receiver.timestamp, this@webSocket)
            }
        }

        webSocket("/user/login") {
            val receiver = receiveDeserialized<ReceiverPayloadLogin>()

            transaction {
                either {
                    receiverRepository.findByName(receiver.name)
                }.onLeft {
                    call.respond(HttpStatusCode.BadRequest, "User name not found")
                }.onRight {
                    connections += it.id to ReceiverSession(it.domain(), receiver.timestamp, this@webSocket)
                    call.respond(HttpStatusCode.OK, "Login successful")
                }
            }
        }

        post("/user/logout") {
            val logout = call.receive<ReceiverPayloadLogout>()
            connections[logout.id]?.let {
                it.session {
                    close(CloseReason(CloseReason.Codes.NORMAL, "User logged out"))
                }
            }
            connections -= logout.id
            call.respond("Logged out user $logout")
        }

        post("/send") {
            val message = call.receive<MessagePayload>()
            either {
                transaction { receiverRepository.load(message.receiver) }
            }.onLeft {
                call.respond(HttpStatusCode.BadRequest, "Receiver not found")
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