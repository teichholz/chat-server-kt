package repository

import arrow.core.raise.Raise

interface Repository<CLASS, ID> {
    suspend fun save(obj: CLASS): ID

    context(Raise<SqlError.RecordNotFound>)
    suspend fun load(id: ID): CLASS

    context(Raise<SqlError.RecordNotFound>)
    suspend fun saveAndLoad(obj: CLASS): CLASS = load(save(obj))
}