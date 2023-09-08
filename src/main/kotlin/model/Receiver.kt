package model

import io.ktor.server.auth.*

data class Receiver(val id: Int, val name: String) : Principal {}