package repository

import arrow.core.raise.Raise

interface Repository<CLASS, ID> {
    suspend fun save(obj: CLASS): CLASS

    context(Raise<SqlError.RecordNotFound>)
    suspend fun delete(id: ID)

    context(Raise<SqlError.RecordNotFound>)
    suspend fun load(id: ID): CLASS
}