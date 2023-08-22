package routing

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.Serializable
import model.tables.records.MessageRecord

@Serializable
data class MessagePayload(val from: ReceiverPayloadWithId, val message: String, val date: LocalDateTime, val receiver: Int)

@Serializable
data class MessagePayloadJob(val from: ReceiverPayload, val message: String, val date: LocalDateTime)

fun MessagePayload.record(): MessageRecord = MessageRecord()
    .setContent(message)
    .setDate(date.toJavaLocalDateTime())
    .setSender(from.id)
    .setReceiver(receiver)
    .setSent(false)