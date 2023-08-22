package repository

import arrow.core.raise.Raise
import com.zaxxer.hikari.HikariDataSource
import di.di
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import model.Message
import model.Tables.MESSAGE
import model.tables.records.MessageRecord
import org.jooq.Configuration
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import reactor.core.publisher.Flux


@Single
class MessageRepositoryImpl(db: HikariDataSource) : MessageRepository(db) {
    context(Configuration)
    override suspend fun unsent(receiverId: Int): Flow<MessageRecord> = sql {
        val query = select()
            .from(MESSAGE)
            .where(MESSAGE.RECEIVER.eq(receiverId).and(MESSAGE.SENT.eq(false)))

        Flux.from(query).asFlow().map {
            it.into(MessageRecord::class.java)
        }
    }
}

fun Message.record(): MessageRecord = MessageRecord()
    .setContent(content)
    .setDate(date.toJavaLocalDateTime())
    .setSender(sender.id)
    .setReceiver(receiver.id)
    .setSent(sent)

context(Raise<SqlError.RecordNotFound>, Configuration)
suspend inline fun MessageRecord.domain(): Message {
    val receiverRepository = di { inject<ReceiverRepositoryImpl>().value }
    return sql {
        val sender = receiverRepository.load(this@domain.sender)
        val receiver = receiverRepository.load(this@domain.receiver)
        Message(content, date.toKotlinLocalDateTime(), sender.domain(), receiver.domain(), sent)
    }
}