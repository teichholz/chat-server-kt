package repository

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import model.Tables
import model.tables.ReceiverJ.RECEIVER
import model.tables.records.ReceiverRecord
import org.jooq.Configuration
import org.koin.core.annotation.Single
import reactor.core.publisher.Flux


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

    context(Configuration)
    override suspend fun users(): Flow<ReceiverRecord> {
        return sql {
            val query = select()
                .from(RECEIVER)

            Flux.from(query).asFlow().map {
                it.into(ReceiverRecord::class.java)
            }
        }
    }
}

fun ReceiverRecord.domain(): model.Receiver = model.Receiver(id, name)

fun model.Receiver.record(): ReceiverRecord = ReceiverRecord(null, name)