package model

import arrow.core.raise.Raise
import chat.commons.routing.MessagePayloadPOST
import kotlinx.datetime.LocalDateTime
import model.tables.records.MessageRecord
import org.jooq.Configuration
import repository.SqlError
import repository.domain


data class Message(val content: String, val date: LocalDateTime, val sender: Receiver, val receiver: Receiver)

fun MessagePayloadPOST.record(receiver: Int): MessageRecord = MessageRecord()
    .setContent(message)
    .setDate(java.time.LocalDateTime.now())
    .setSender(from.id)
    .setReceiver(receiver)

context(Raise<SqlError.RecordNotFound>, Configuration)
suspend fun MessagePayloadPOST.domain(receiver: Int): Message = record(receiver).domain()