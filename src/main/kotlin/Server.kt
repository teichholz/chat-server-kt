import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import chat.commons.protocol.Protocol
import chat.commons.protocol.ack
import chat.commons.protocol.auth
import chat.commons.protocol.isAuth
import chat.commons.routing.MessagePayloadPOST
import chat.commons.routing.ReceiverPayload
import chat.commons.routing.ReceiverPayloadLogin
import chat.commons.routing.ReceiverPayloadLogout
import chat.commons.routing.ReceiverPayloadRegister
import chat.commons.routing.ReceiverPayloadWithId
import di.di
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.json.Json
import logger.getLogger
import model.Receiver
import model.record
import model.tables.records.MessageRecord
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
import scheduler.Connections
import scheduler.DeliverMessagesJob
import scheduler.DeliverMessagesJob.connections
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
        modules(RepositoryModule().module, SchedulerModule().module)
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

    routing {
        post("/users/register") {
            val receiver = call.receive<ReceiverPayloadRegister>()

            logger.info("Received register call with $receiver")

            transaction {
                either {
                    receiverRepository.findByName(receiver.name).domain()
                }.onLeft {
                    val domain = receiverRepository.save(ReceiverRecord(null, receiver.name)).domain()
                    val response = ReceiverPayloadWithId(domain.id, domain.name)
                    call.respond(HttpStatusCode.OK, response)
                }.onRight {
                    call.respond(HttpStatusCode.BadRequest, "User name already taken")
                }
            }
        }

        post("/users/login") {
            val receiver = call.receive<ReceiverPayloadLogin>()

            logger.info("Received login call with $receiver")

            transaction {
                either {
                    receiverRepository.findByName(receiver.name).domain()
                }.onLeft {
                    call.respond(HttpStatusCode.BadRequest, "User name not found")
                    logger.info("User name ${receiver.name} not found");
                }.onRight {
                    val response = ReceiverPayloadWithId(it.id, it.name)
                    call.respond(HttpStatusCode.OK, response)
                    logger.info("User properly logged in: $response")
                }
            }
        }

        webSocket("/receive") {
            authenticated(this) {}
        }

        webSocket("/send") {
            authenticated(this) {
                while (isActive) {
                    val message = receiveDeserialized<Protocol.MESSAGE>()
                    val receiver = message.payload.to
                    val sender = message.payload.from
                    val content = message.payload.message
                    val date = message.payload.sent

                    logger.info("Received message from $sender to $receiver with content $content")

                    either {
                        transaction {
                            val receiverRecord = receiverRepository.findByName(receiver.name)
                            val senderRecord = receiverRepository.findByName(sender.name)
                            messageRepository.save(
                                MessageRecord()
                                    .setContent(content)
                                    .setDate(date.toJavaLocalDateTime())
                                    .setSender(senderRecord.id)
                                    .setReceiver(receiverRecord.id)
                            )
                        }
                        sendSerialized(ack {})
                    }.onLeft {
                        logger.error("Error saving received message: $it")
                    }
                }
            }
        }


        post("/users/logout") {
            val logout = call.receive<ReceiverPayloadLogout>()
            connections[logout.id]?.let {
                it.session {
                    close(CloseReason(CloseReason.Codes.NORMAL, "User logged out"))
                }
            }
            connections -= logout.id
        }

        get("/users/registered") {
            val users = transaction {
                receiverRepository.users().map {
                    ReceiverPayload(it.name)
                }.toList()
            }

            call.respond(users)
        }


        post("/send") {
            val message = call.receive<MessagePayloadPOST>()
            either {
                transaction {
                    receiverRepository.findByName(message.to.name)
                }
            }.onLeft {
                call.respond(HttpStatusCode.BadRequest, "Receiver not found")
            }.onRight {
                transaction {
                    messageRepository.save(message.record(it.id))
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


suspend fun authenticated(
    session: DefaultWebSocketServerSession,
    block: suspend DefaultWebSocketServerSession.() -> Unit
): Nothing {
    var auth: Protocol.AUTH? = null

    try {
        val protocol = session.receiveDeserialized<Protocol>()

        if (!protocol.isAuth()) {
            session.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Expected AUTH Message but got ${protocol.kind}"))
            throw IllegalStateException("Expected AUTH Message but got ${protocol.kind}")
        }

        auth = protocol as Protocol.AUTH
        val domain = Receiver(protocol.payload.receiver.id, protocol.payload.receiver.name)
        connections += protocol.payload.receiver.id to ReceiverSession(domain, protocol.payload.lastMessage, session)
        session.sendSerialized(ack {})

        block.invoke(session)

        awaitCancellation()
    } catch (e: Throwable) {
        logger.error("Error in authenticated block: $e")
        throw e
    } finally {
        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server is shutting down"))
        auth?.auth {
            connections -= it.payload.receiver.id
        }
    }
}