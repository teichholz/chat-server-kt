package repository


sealed interface SqlError {

    data class RecordNotFound(val id: Any): SqlError {
        val message: String
            get() = "Record not found: $id"
    }

    data class Unexpected(val exception: Throwable): SqlError
}

