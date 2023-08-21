package repository

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.future.asDeferred
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.UpdatableRecord
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.jooq.kotlin.coroutines.transactionCoroutine

abstract class JooqRepository<RECORD : UpdatableRecord<RECORD>>(val database: HikariDataSource, val table: TableImpl<RECORD>) :  Repository<RECORD, Int> {

    override suspend fun save(obj: RECORD): RECORD {
        return sql {
            var attached = obj
            if (obj.configuration() == null) {
                attached = newRecord(table, obj)
            }
            attached.store()
            attached.into(table)
        }
    }

    context(Raise<SqlError.RecordNotFound>)
    override suspend fun delete(id: Int): Unit = sql {
        val count = deleteFrom(table).where(table.field(0, Int::class.java)!!.eq(id)).executeAsync().asDeferred().await()

        ensure(count == 1) {
            raise(SqlError.RecordNotFound(id))
        }
    }

    context(Raise<SqlError.RecordNotFound>)
    override suspend fun load(id: Int): RECORD = sql {
        fetchOne(table, table.field(0, Int::class.java)!!.eq(id)) ?: raise(SqlError.RecordNotFound(id))
    }

    suspend fun <T> sql(block: suspend DSLContext.() -> T): T {
        return transaction {
            block(dsl())
        }
    }

    suspend fun <T> transaction(block: suspend Configuration.() -> T): T {
        val dsl = DSL.using(database, SQLDialect.POSTGRES)

        return dsl.transactionCoroutine { ctx: Configuration ->
            block(ctx)
        }
    }
}