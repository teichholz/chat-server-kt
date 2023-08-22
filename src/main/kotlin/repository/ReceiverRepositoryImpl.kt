package repository

import com.zaxxer.hikari.HikariDataSource
import model.tables.records.ReceiverRecord
import org.koin.core.annotation.Single


@Single
class ReceiverRepositoryImpl(db: HikariDataSource) : ReceiverRepository(db) {
}

fun ReceiverRecord.domain(): model.Receiver = model.Receiver(id, name)

fun model.Receiver.record(): ReceiverRecord = ReceiverRecord(null, name)