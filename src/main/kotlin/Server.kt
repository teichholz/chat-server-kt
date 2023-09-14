import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.either
import arrow.fx.coroutines.resourceScope
import chat.commons.protocol.MessagePayloadSocket
import chat.commons.protocol.Protocol
import chat.commons.protocol.ack
import chat.commons.protocol.auth
import chat.commons.protocol.isAuth
import chat.commons.protocol.message
import chat.commons.routing.ReceiverPayload
import chat.commons.routing.ReceiverPayloadLogin
import chat.commons.routing.ReceiverPayloadLogout
import chat.commons.routing.ReceiverPayloadRegister
import chat.commons.routing.ReceiverPayloadWithId
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.json.Json
import logger.getLogger
import model.Receiver
import model.tables.records.MessageRecord
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
import repository.unsafeDomain
import scheduler.Connections
import scheduler.JobRegistry
import scheduler.ReceiverSession
import scheduler.Scheduler
import scheduler.SchedulerModule
import scheduler.currentSession

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
        modules(RepositoryModule().module, SchedulerModule().module)
        logger.info("Koin installed")
    }
}

fun Application.logging() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            call.request.toLogString()
            val path = call.request.path()
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val authorization = call.request.authorization()
            val userAgent = call.request.headers["User-Agent"]
            "Path: $path, Status: $status, HTTP method: $httpMethod, Authorization: $authorization, User agent: $userAgent"
        }

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
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Error in call: $call", cause)
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    val messageRepository: MessageRepository by inject()
    val receiverRepository: ReceiverRepository by inject()

    install(Authentication) {
        basic("basic-auth") {
            realm = "Access to the '/' path"
            validate { credentials ->
                either {
                    transaction {
                        receiverRepository.findByName(credentials.name)
                    }
                }.fold({ null }) {
                    Receiver(it.id, it.name)
                }
            }
        }
    }

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

        authenticate("basic-auth") {
            webSocket("/receive") {
                keepConnection {
                    val session = currentSession()

                    while (isActive) {
                        val messages = transaction {
                            messageRepository.sortedMessagesWithOffset(session.receiver.id, connections.messageCount())
                        }

                        logger.info("Found unsent messages: $messages")

                        messages.forEach {
                            logger.info("Found unsent message: $it")
                            val msg = it.unsafeDomain()
                            sendSerialized(message {
                                this.payload = MessagePayloadSocket(
                                    ReceiverPayload(msg.sender.name),
                                    ReceiverPayload(msg.receiver.name),
                                    msg.content,
                                    msg.date
                                )
                            })

                            val protocol: Protocol = receiveDeserialized()
                            logger.info("Send message and received ACK: $protocol")
                            when (protocol) {
                                is Protocol.ACK -> {
                                    connections.incrementMessageCount()
                                }

                                else -> throw IllegalStateException("SHOULD NOT HAPPEN")
                            }
                        }

                        delay(5000)
                    }
                }
            }
        }

        authenticate("basic-auth") {
            webSocket("/send") {
                keepConnection {
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
                                    MessageRecord().apply {
                                        this.content = content
                                        this.date = date.toJavaLocalDateTime()
                                        this.sender = senderRecord.id
                                        this.receiver = receiverRecord.id
                                    }
                                )
                            }
                            sendSerialized(ack {})
                            connections.incrementMessageCount()

                        }.onLeft {
                            logger.error("Error saving received message: $it")
                        }
                    }
                }
            }
        }


        authenticate("basic-auth") {
            post("/users/logout") {
                val logout = call.receive<ReceiverPayloadLogout>()
                connections -= logout.id
            }
        }

        authenticate("basic-auth") {
            get("/users/registered") {
                val users = transaction {
                    receiverRepository.users().map {
                        ReceiverPayload(it.name)
                    }.filter {
                        it.name != call.principal<Receiver>()!!.name
                    }.toList()
                }

                call.respond(users)
            }
        }
    }
}


context(Route)
suspend fun DefaultWebSocketServerSession.keepConnection(
    block: suspend DefaultWebSocketServerSession.() -> Unit
) {
    val principal: Receiver = call.principal()!!
    val connections by inject<Connections>()

    var auth: Protocol.AUTH? = null
    try {
        val protocol = receiveDeserialized<Protocol>()

        if (!protocol.isAuth()) {
            close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Expected AUTH Message but got $protocol"))
            throw IllegalStateException("Expected AUTH Message but got $protocol")
        }

        auth = protocol as Protocol.AUTH
        connections += principal.id to ReceiverSession(principal, protocol.payload.lastMessage)
        sendSerialized(ack {})

        block()

        awaitCancellation()
    } catch (e: Throwable) {
        if (e is ClosedReceiveChannelException || e is ClosedSendChannelException) {
            logger.info("Websocket connection closed for ${principal.name}")
        } else {
            logger.error("Error in keepConnection block: $e")
            throw e
        }
    } finally {
        close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server is shutting down"))
        auth?.auth {
            connections -= principal.id
        }
    }
}
