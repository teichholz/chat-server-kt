package repository

import arrow.core.raise.Raise
import com.zaxxer.hikari.HikariDataSource
import di.dis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import model.Message
import model.Tables.MESSAGE
import model.tables.records.MessageRecord
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import reactor.core.publisher.Flux


@Single
class MessageRepositoryDB(db: HikariDataSource) : JooqRepository<MessageRecord>(db, MESSAGE), MessageRepository {
    override suspend fun unsent(receiverId: Int): Flow<MessageRecord> = sql {
        val query = select()
            .from(MESSAGE)
            .where(MESSAGE.receiver().ID.eq(receiverId).and(MESSAGE.SENT.eq(false)))

        Flux.from(query).asFlow().map {
            it.into(MessageRecord::class.java)
        }
    }
}

fun Message.map(): MessageRecord = MessageRecord(null, content, receiver.id, sent)

context(Raise<SqlError.RecordNotFound>)
suspend fun MessageRecord.map(): Message = dis {
    val receiverRepository by inject<ReceiverRepositoryDB>()
    val receiver = receiverRepository.load(this.receiver)
    Message(content, receiver.map(), sent)
}