package repository

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.zaxxer.hikari.HikariDataSource
import di.di
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.UpdatableRecord
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.koin.core.component.inject

abstract class JooqRepository<RECORD : UpdatableRecord<RECORD>>(val db: HikariDataSource, val table: TableImpl<RECORD>) :  Repository<RECORD, Int> {

    context(Configuration)
    override suspend fun save(obj: RECORD): RECORD {
        var attached = obj
        if (obj.configuration() == null) {
            attached = dsl().newRecord(table, obj)
        }
        attached.store()
        return attached
    }

    context(Raise<SqlError.RecordNotFound>, Configuration)
    override suspend fun delete(id: Int): Unit {
        val count = dsl()
            .deleteFrom(table).where(table.field(0, Int::class.java)!!.eq(id))
                .executeAsync()
                .await()

        ensure(count == 1) {
            raise(SqlError.RecordNotFound(id))
        }
    }

    context(Raise<SqlError.RecordNotFound>, Configuration)
    override suspend fun load(id: Int): RECORD {
        val await = dsl().fetchAsync(table, table.field(0, Int::class.java)!!.eq(id)).await()
        ensure(await.size == 1) {
            raise(SqlError.RecordNotFound(id))
        }
        return await.first()
    }


    suspend fun <T> transaction(block: suspend Configuration.() -> T): T {
        val dsl = DSL.using(db, SQLDialect.POSTGRES)

        return dsl.transactionCoroutine { ctx: Configuration ->
            block(ctx)
        }
    }
}

suspend fun <T> Configuration.sql(block: suspend DSLContext.() -> T): T = block(dsl())

suspend fun <T> transaction(block: suspend Configuration.() -> T): T {
    val db = di { inject<HikariDataSource>().value }
    val dsl = DSL.using(db, SQLDialect.POSTGRES)

    return dsl.transactionCoroutine { ctx: Configuration ->
        block(ctx)
    }
}
