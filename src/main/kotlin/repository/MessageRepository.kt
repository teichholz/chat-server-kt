package repository

import kotlinx.coroutines.flow.Flow
import model.tables.records.MessageRecord
import model.tables.records.ReceiverRecord

/**
 * T: Type of the id
 */
interface MessageRepository<T> : Repository<MessageRecord, T> {
    suspend fun unsent(receiver: ReceiverRecord): Flow<MessageRecord>
}