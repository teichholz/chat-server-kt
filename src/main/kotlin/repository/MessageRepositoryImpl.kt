package repository

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.zaxxer.hikari.HikariDataSource
import di.di
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import model.Message
import model.tables.MessageJ.MESSAGE
import model.tables.records.MessageRecord
import org.jooq.Configuration
import org.koin.core.annotation.Single
import org.koin.core.component.inject


@Single
class MessageRepositoryImpl(db: HikariDataSource) : MessageRepository(db) {
    context(Configuration)
    override suspend fun messagesAfterTimestamp(receiverId: Int, after: LocalDateTime?): List<MessageRecord> {
        val timestamp = after?.toJavaLocalDateTime() ?: java.time.LocalDateTime.MIN

        return sql {
            val query = select()
                .from(MESSAGE)
                .where(MESSAGE.RECEIVER.eq(receiverId).and(MESSAGE.DATE.gt(timestamp)))
                .orderBy(MESSAGE.DATE.asc())

            query.map {
                it.into(MessageRecord::class.java)
            }
        }
    }

    context(Configuration)
    override suspend fun sortedMessagesWithOffset(receiverId: Int, offset: Long): List<MessageRecord> {
        return sql {
            val query = select()
                .from(MESSAGE)
                .where(MESSAGE.RECEIVER.eq(receiverId).or(MESSAGE.SENDER.eq(receiverId)))
                .orderBy(MESSAGE.DATE.asc())
                .offset(offset)

            query.map {
                it.into(MessageRecord::class.java)
            }
        }
    }
}

fun Message.record(): MessageRecord = MessageRecord()
    .setContent(content)
    .setDate(date.toJavaLocalDateTime())
    .setSender(sender.id)
    .setReceiver(receiver.id)

context(Raise<SqlError.RecordNotFound>, Configuration)
suspend inline fun MessageRecord.domain(): Message {
    val message = this
    val receiverRepository = di { inject<ReceiverRepositoryImpl>().value }
    return sql {
        val sender = receiverRepository.load(message.sender)
        val receiver = receiverRepository.load(message.receiver)
        Message(content, date.toKotlinLocalDateTime(), sender.domain(), receiver.domain())
    }
}

suspend inline fun MessageRecord.unsafeDomain(): Message {
    return transaction {
        sql {
            either {
                domain()
            }.fold({ throw IllegalStateException("SHOULD NOT HAPPEN") }) { it }
        }
    }
}
