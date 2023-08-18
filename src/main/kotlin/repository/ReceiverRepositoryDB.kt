package repository

import com.zaxxer.hikari.HikariDataSource
import model.Tables.RECEIVER
import model.tables.records.ReceiverRecord
import org.koin.core.annotation.Single


@Single
class ReceiverRepositoryDB(db: HikariDataSource) : JooqRepository<ReceiverRecord>(db, RECEIVER), ReceiverRepository<Int> {
}