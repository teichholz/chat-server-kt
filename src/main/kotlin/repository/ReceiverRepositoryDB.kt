package repository

import com.zaxxer.hikari.HikariDataSource
import model.Tables.RECEIVER
import model.tables.records.ReceiverRecord
import org.koin.core.annotation.Single


@Single
class ReceiverRepositoryDB(db: HikariDataSource) : ReceiverRepository(db) {
}

fun ReceiverRecord.map(): model.Receiver = model.Receiver(id, name)

fun model.Receiver.map(): ReceiverRecord = ReceiverRecord(null, name)