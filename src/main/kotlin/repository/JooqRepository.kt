package repository

import arrow.core.raise.Raise
import com.zaxxer.hikari.HikariDataSource
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.UpdatableRecord
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl

abstract class JooqRepository<RECORD : UpdatableRecord<RECORD>>(val database: HikariDataSource, val table: TableImpl<RECORD>) :  Repository<RECORD, Int> {

    override suspend fun save(obj: RECORD): Int {
        return sql {
            val attached = newRecord(table, obj)
            attached.store()
            attached.get(0, Int::class.java)
        }
    }

    context(Raise<SqlError.RecordNotFound>)
    override suspend fun load(id: Int): RECORD = sql {
        fetchOne(table, table.field(0, Int::class.java)!!.eq(id)) ?: raise(SqlError.RecordNotFound(id))
    }

    fun <T> sql(block: DSLContext.() -> T): T {
        return transaction {
            block(dsl())
        }
    }

    fun <T> transaction(block: Configuration.() -> T): T {
        val dsl = DSL.using(database, SQLDialect.POSTGRES)

        return dsl.transactionResult{ ctx: Configuration ->
            block(ctx)
        }
    }
}