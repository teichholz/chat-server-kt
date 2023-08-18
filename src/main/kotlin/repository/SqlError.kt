package repository


sealed class SqlError(override val message: String) : Throwable(message) {
    data class RecordNotFound(val id: Int) : SqlError("Record not found: $id") {}
}

