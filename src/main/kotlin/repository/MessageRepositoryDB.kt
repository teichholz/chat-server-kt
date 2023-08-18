package repository

import di.Instances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import model.Tables.MESSAGE
import model.tables.records.MessageRecord
import model.tables.records.ReceiverRecord
import reactor.core.publisher.Flux


class MessageRepositoryDB : JooqRepository<MessageRecord>(Instances.database, MESSAGE), MessageRepository<Int> {
    override suspend fun unsent(receiver: ReceiverRecord): Flow<MessageRecord> = sql {
        val query = select()
            .from(MESSAGE)
            .where(MESSAGE.receiver().ID.eq(receiver.id).and(MESSAGE.SENT.eq(false)))

        Flux.from(query).asFlow().map {
            it.into(MessageRecord::class.java)
        }
    }
}