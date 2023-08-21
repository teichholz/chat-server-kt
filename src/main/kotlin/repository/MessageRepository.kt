package repository

import kotlinx.coroutines.flow.Flow
import model.tables.records.MessageRecord
import model.tables.records.ReceiverRecord

/**
 * T: Type of the id
 */
interface MessageRepository : Repository<MessageRecord, Int> {
    suspend fun unsent(receiverId: Int): Flow<MessageRecord>
}