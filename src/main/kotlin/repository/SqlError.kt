package repository


sealed class SqlError(override val message: String) : Throwable(message) {
    data class RecordNotFound(val id: Any) : SqlError("Record not found: $id") {}
}

