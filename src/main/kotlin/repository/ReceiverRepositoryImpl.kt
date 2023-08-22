package repository

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.future.await
import model.Tables
import model.tables.records.ReceiverRecord
import org.jooq.Configuration
import org.koin.core.annotation.Single


@Single
class ReceiverRepositoryImpl(db: HikariDataSource) : ReceiverRepository(db) {

    context(Raise<SqlError.RecordNotFound>, Configuration)
    override suspend fun findByName(name: String): ReceiverRecord {
        val dsl = dsl();
        val records = dsl.fetchAsync(Tables.RECEIVER, Tables.RECEIVER.NAME.eq("name")).await()

        ensure(records.size == 1) {
            raise(SqlError.RecordNotFound(name))
        }

        return records.first()
    }
}

fun ReceiverRecord.domain(): model.Receiver = model.Receiver(id, name)

fun model.Receiver.record(): ReceiverRecord = ReceiverRecord(null, name)