package repository

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.zaxxer.hikari.HikariDataSource
import model.Tables
import model.tables.records.ReceiverRecord
import org.jooq.Configuration

/**
 * T: Type of the id
 */
abstract class ReceiverRepository(db: HikariDataSource) : JooqRepository<ReceiverRecord>(db, Tables.RECEIVER) {
    context(Raise<SqlError.RecordNotFound>, Configuration)
    abstract suspend fun findByName(name: String): ReceiverRecord

    context(Configuration)
    suspend fun isNameTaken(name: String): Boolean {
        return either {
            findByName(name)
        }.fold({ it is SqlError.RecordNotFound }) { false }
    }

    context(Configuration)
    suspend fun isNameFree(name: String): Boolean = !isNameTaken(name)
}