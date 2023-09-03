package repository

import arrow.core.raise.Raise
import com.zaxxer.hikari.HikariDataSource
import di.di
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import model.Message
import model.tables.MessageJ.MESSAGE
import model.tables.records.MessageRecord
import org.jooq.Configuration
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import reactor.core.publisher.Flux


@Single
class MessageRepositoryImpl(db: HikariDataSource) : MessageRepository(db) {
    context(Configuration)
    override suspend fun messagesAfterTimestamp(receiverId: Int, after: LocalDateTime?): Flow<MessageRecord> {
        val timestamp = after?.toJavaLocalDateTime() ?: java.time.LocalDateTime.MIN

        return sql {
            val query = select()
                .from(MESSAGE)
                .where(MESSAGE.RECEIVER.eq(receiverId).and(MESSAGE.DATE.gt(timestamp)))
                .orderBy(MESSAGE.DATE.asc())

            Flux.from(query).asFlow().map {
                it.into(MessageRecord::class.java)
            }
        }
    }

    context(Configuration)
    override suspend fun sortedMessagesWithOffset(receiverId: Int, offset: Long): Flow<MessageRecord>  {
        return sql {
            val query = select()
                .from(MESSAGE)
                .where(MESSAGE.RECEIVER.eq(receiverId))
                .orderBy(MESSAGE.DATE.asc())
                .offset(offset)

            Flux.from(query).asFlow().map {
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