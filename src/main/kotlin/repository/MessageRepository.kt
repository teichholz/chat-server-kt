package repository

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.flow.Flow
import model.Tables
import model.tables.records.MessageRecord
import java.lang.module.Configuration

/**
 * T: Type of the id
 */
abstract class MessageRepository(db: HikariDataSource) : JooqRepository<MessageRecord>(db, Tables.MESSAGE) {
    context(Configuration)
    abstract suspend fun unsent(receiverId: Int): Flow<MessageRecord>
}