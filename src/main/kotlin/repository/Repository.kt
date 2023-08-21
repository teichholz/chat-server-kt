package repository

import arrow.core.raise.Raise
import org.jooq.Configuration

interface Repository<CLASS, ID> {
    context(Configuration)
    suspend fun save(obj: CLASS): CLASS

    context(Raise<SqlError.RecordNotFound>, Configuration)
    suspend fun delete(id: ID)

    context(Raise<SqlError.RecordNotFound>, Configuration)
    suspend fun load(id: ID): CLASS
}