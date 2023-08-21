package repository

import com.zaxxer.hikari.HikariDataSource
import model.Tables
import model.tables.records.ReceiverRecord

/**
 * T: Type of the id
 */
abstract class ReceiverRepository(db: HikariDataSource) : JooqRepository<ReceiverRecord>(db, Tables.RECEIVER) {
}