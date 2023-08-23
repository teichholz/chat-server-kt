package routing

import arrow.core.raise.Raise
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.Serializable
import model.Message
import model.tables.records.MessageRecord
import org.jooq.Configuration
import repository.SqlError
import repository.domain

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

context(Raise<SqlError.RecordNotFound>, Configuration)
suspend fun MessagePayload.domain(): Message = record().domain()
